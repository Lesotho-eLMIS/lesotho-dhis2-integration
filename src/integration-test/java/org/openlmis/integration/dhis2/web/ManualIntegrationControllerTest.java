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

package org.openlmis.integration.dhis2.web;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import guru.nidi.ramltester.junit.RamlMatchers;
import java.util.UUID;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.openlmis.integration.dhis2.service.PayloadService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@SuppressWarnings("PMD.TooManyMethods")
public class ManualIntegrationControllerTest extends BaseWebIntegrationTest {

  private static final String RESOURCE_URL = "/api/integrationExecutions";
  private static final String PROGRAMID = "programId";
  private static final String PERIODID = "periodId";
  private static final String FACILITYID = "facilityId";
  private ManualIntegrationDto manualIntegrationDto = generateRequestBody();

  @MockBean
  PayloadService payloadService;

  @Test
  public void shouldCreateRequest() {

    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .body(manualIntegrationDto)
        .when()
        .post(RESOURCE_URL)
        .then()
        .statusCode(HttpStatus.SC_CREATED)
        .body(PROGRAMID, is(manualIntegrationDto.getProgramId().toString()))
        .body(PERIODID, is(manualIntegrationDto.getPeriodId().toString()))
        .body(FACILITYID, is(manualIntegrationDto.getFacilityId().toString()));

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnUnauthorizedForCreateRequestEndpointIfUserIsNotAuthorized() {
    restAssured
        .given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .body(manualIntegrationDto)
        .when()
        .post(RESOURCE_URL)
        .then()
        .statusCode(HttpStatus.SC_UNAUTHORIZED);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  private ManualIntegrationDto generateRequestBody() {
    ManualIntegrationDto dto = new ManualIntegrationDto();
    dto.setProgramId(UUID.randomUUID());
    dto.setPeriodId(UUID.randomUUID());
    dto.setFacilityId(UUID.randomUUID());
    return dto;
  }

}