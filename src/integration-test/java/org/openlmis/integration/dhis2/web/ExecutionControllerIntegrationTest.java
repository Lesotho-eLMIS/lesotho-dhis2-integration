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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;

import com.google.common.collect.Lists;

import guru.nidi.ramltester.junit.RamlMatchers;

import java.util.Arrays;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.integration.dhis2.ExecutionDataBuilder;
import org.openlmis.integration.dhis2.IntegrationDataBuilder;
import org.openlmis.integration.dhis2.domain.Execution;
import org.openlmis.integration.dhis2.domain.Integration;
import org.openlmis.integration.dhis2.i18n.MessageKeys;
import org.openlmis.integration.dhis2.service.referencedata.ProcessingPeriodDto;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@SuppressWarnings("PMD.TooManyMethods")
public class ExecutionControllerIntegrationTest extends BaseWebIntegrationTest {

  private static final String RESOURCE_URL = ExecutionController.RESOURCE_PATH;
  private static final String ID_URL = RESOURCE_URL + ExecutionController.ID_URL;
  private static final String PROGRAM_ID = "programId";
  private static final String PERIOD_ID = "periodId";
  private static final String FACILITY_ID = "facilityId";

  private Execution execution = new ExecutionDataBuilder().buildAsAutomatic();
  private Execution execution1 = new ExecutionDataBuilder().buildAsManual();

  private ExecutionDto executionDto = ExecutionDto.newInstance(execution);

  private ManualIntegrationDto manualIntegrationDto = generateRequestBody();

  private ProcessingPeriodDto period =  new ProcessingPeriodDto();

  private Integration integration = new IntegrationDataBuilder().build();

  /** Set up sample data.
   */

  @Before
  public void setUp() {

    given(executionRepository
        .findAll(any(Pageable.class)))
        .willReturn(new PageImpl<>(Lists.newArrayList(execution, execution1)));

    given(integrationRepository
        .findByProgramId(manualIntegrationDto.getProgramId()))
        .willReturn(integration);

    given(periodReferenceDataService
        .findOne(manualIntegrationDto.getPeriodId()))
        .willReturn(period);

    willDoNothing().given(permissionService).canManageDhis2();
  }


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
        .body(PROGRAM_ID, is(manualIntegrationDto.getProgramId().toString()))
        .body(PERIOD_ID, is(manualIntegrationDto.getPeriodId().toString()))
        .body(FACILITY_ID, is(manualIntegrationDto.getFacilityId().toString()));

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

  @Test
  public void shouldReturnPageOfExecutions() {
    given(executionRepository.findAll(any(Pageable.class)))
        .willReturn(new PageImpl<>(Arrays.asList(execution, execution1)));

    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .queryParam("page", pageable.getPageNumber())
        .queryParam("size", pageable.getPageSize())
        .when()
        .get(RESOURCE_URL)
        .then()
        .statusCode(HttpStatus.SC_OK)
        .body("content", hasSize(2))
        .body("content[0].programId", is(executionDto.getProgramId().toString()));

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnUnauthorizedForAllExecutionEndpointIfUserIsNotAuthorized() {
    restAssured.given()
        .when()
        .get(RESOURCE_URL)
        .then()
        .statusCode(HttpStatus.SC_UNAUTHORIZED);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(),RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnForbiddenWhenUserHasNotRightForGetAllEntries() {
    disablePermission();

    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .when()
        .get(RESOURCE_URL)
        .then()
        .statusCode(org.springframework.http.HttpStatus.FORBIDDEN.value())
        .body(MESSAGE_KEY, is(MessageKeys.ERROR_PERMISSION_MISSING));

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnGivenExecution() {
    given(executionRepository.findOne(executionDto.getId())).willReturn(execution);

    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .pathParam(ID, executionDto.getId().toString())
        .when()
        .get(ID_URL)
        .then()
        .statusCode(HttpStatus.SC_OK)
        .body("id", is(executionDto.getId().toString()))
        .body("programId", is(executionDto.getProgramId().toString()));

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnUnauthorizedForGetSpecifiedExecutionIfUserIsNotAuthorized() {
    given(executionRepository.findOne(executionDto.getId())).willReturn(execution);

    restAssured
        .given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .pathParam(ID, executionDto.getId().toString())
        .when()
        .get(ID_URL)
        .then()
        .statusCode(HttpStatus.SC_UNAUTHORIZED);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnForbiddenWhenUserHasNotRightForGetExecutionById() {
    disablePermission();

    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .pathParam(ID, executionDto.getId())
        .when()
        .get(ID_URL)
        .then()
        .statusCode(org.springframework.http.HttpStatus.FORBIDDEN.value())
        .body(MESSAGE_KEY, is(MessageKeys.ERROR_PERMISSION_MISSING));

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnNotFoundWhenExecutionWithIdDoesNotExistForGetExecutionById() {
    given(executionRepository.findOne(executionDto.getId())).willReturn(null);

    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .pathParam(ID, executionDto.getId())
        .when()
        .get(ID_URL)
        .then()
        .statusCode(org.springframework.http.HttpStatus.NOT_FOUND.value())
        .body(MESSAGE_KEY, is(MessageKeys.ERROR_EXECUTION_NOT_FOUND));

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  private void disablePermission() {
    willThrow(new MissingPermissionException("permission"))
        .given(permissionService)
        .canManageDhis2();
  }

  private ManualIntegrationDto generateRequestBody() {
    ManualIntegrationDto dto = new ManualIntegrationDto();
    dto.setProgramId(UUID.randomUUID());
    dto.setPeriodId(UUID.randomUUID());
    dto.setFacilityId(UUID.randomUUID());
    return dto;
  }

}