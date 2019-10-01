/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.integration.dhis2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.ZonedDateTime;
import org.openlmis.integration.dhis2.domain.Execution;
import org.openlmis.integration.dhis2.domain.ExecutionResponse;
import org.openlmis.integration.dhis2.repository.ExecutionRepository;
import org.openlmis.integration.dhis2.service.referencedata.ProcessingPeriodDto;
import org.openlmis.integration.dhis2.service.referencedata.ProgramDto;
import org.openlmis.integration.dhis2.service.referencedata.ProgramReferenceDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Service
public class PayloadService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PayloadService.class);

  @Autowired
  private ExecutionRepository executionRepository;

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  @Autowired
  private PayloadBuilder payloadBuilder;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private Clock clock;

  private RestTemplate restTemplate = new RestTemplate();

  /**
   * Method is responsible for sending payload to Interop layer. Response is a status (202, 500 or
   * 503), message and notificationsChannel.
   */
  @Async
  public void postPayload(PayloadRequest payloadRequest) {
    ProgramDto program = programReferenceDataService.findOne(payloadRequest.getProgramId());

    String payloadAsJson;

    try {
      Payload payload = createPayload(payloadRequest, program);
      payloadAsJson = objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exp) {
      throw new IllegalStateException(exp);
    }

    Execution execution = payloadRequest.createExecution(payloadAsJson, clock);
    executionRepository.saveAndFlush(execution);

    ExecutionResponse response = sendPayload(payloadRequest, payloadAsJson);

    execution.markAsDone(response, clock);
    executionRepository.saveAndFlush(execution);

    LOGGER.info("Response status: {}; Message: {}", response.getStatusCode(), response.getBody());
  }

  private ExecutionResponse sendPayload(PayloadRequest request, String body) {
    try {
      RequestHeaders headers = RequestHeaders
          .init()
          .set(HttpHeaders.AUTHORIZATION, request.getAuthorizationHeader())
          .set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
      HttpEntity<String> entity = RequestHelper.createEntity(headers, body);

      ResponseEntity<String> response = restTemplate
          .exchange(request.getTargetUrl(), HttpMethod.POST, entity, String.class);

      return new ExecutionResponse(ZonedDateTime.now(clock), response.getStatusCodeValue(),
          response.getBody());
    } catch (RestClientResponseException exp) {
      return new ExecutionResponse(ZonedDateTime.now(clock), exp.getRawStatusCode(),
          exp.getResponseBodyAsString());
    }
  }

  private Payload createPayload(PayloadRequest request, ProgramDto program) {
    ProcessingPeriodDto period = request.getPeriod();

    return payloadBuilder.build(program.getName(), period.getStartDate(), period.getEndDate(),
        request.getFacilityId()
    );
  }

}
