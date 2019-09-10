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
import static org.mockito.Matchers.any;

import guru.nidi.ramltester.junit.RamlMatchers;

import java.util.Collections;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.openlmis.integration.dhis2.ConfigurationDataBuilder;
import org.openlmis.integration.dhis2.domain.Configuration;
import org.openlmis.integration.dhis2.i18n.MessageKeys;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@SuppressWarnings("PMD.TooManyMethods")
public class ConfigurationControllerIntegrationTest extends BaseWebIntegrationTest {

  private static final String RESOURCE_URL = ConfigurationController.RESOURCE_PATH;
  private static final String ID_URL = RESOURCE_URL + ConfigurationController.ID_URL;

  private Configuration configuration =
      new ConfigurationDataBuilder().withCredentials(UUID.randomUUID().toString()).build();
  private ConfigurationDto configurationDto = ConfigurationDto.newInstance(configuration);

  @Test
  public void shouldCreateConfiguration() {

    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .body(configurationDto)
        .when()
        .post(RESOURCE_URL)
        .then()
        .statusCode(HttpStatus.SC_CREATED);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnUnauthorizedForCreateConfigurationEndpointIfUserIsNotAuthorized() {
    restAssured
        .given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .body(configurationDto)
        .when()
        .post(RESOURCE_URL)
        .then()
        .statusCode(HttpStatus.SC_UNAUTHORIZED);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }


  @Test
  public void shouldReturnPageOfExecutions() {
    given(configurationRepository.findAll(any(Pageable.class)))
        .willReturn(new PageImpl<>(Collections.singletonList(configuration)));

    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .queryParam("page", pageable.getPageNumber())
        .queryParam("size", pageable.getPageSize())
        .when()
        .get(RESOURCE_URL)
        .then()
        .statusCode(HttpStatus.SC_OK)
        .body("content", hasSize(1));

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnUnauthorizedForAllConfigurationsEndpointIfUserIsNotAuthorized() {
    restAssured.given()
        .when()
        .get(RESOURCE_URL)
        .then()
        .statusCode(HttpStatus.SC_UNAUTHORIZED);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnGivenConfiguration() {
    given(configurationRepository.findOne(configurationDto.getId())).willReturn(configuration);

    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .pathParam(ID, configurationDto.getId().toString())
        .when()
        .get(ID_URL)
        .then()
        .statusCode(HttpStatus.SC_OK)
        .body(ID, Matchers.is(configurationDto.getId().toString()));

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnNotFoundMessageIfConfigurationDoesNotExistForGivenConfiguration() {
    given(configurationRepository.findOne(configurationDto.getId())).willReturn(null);

    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .pathParam(ID, configurationDto.getId().toString())
        .when()
        .get(ID_URL)
        .then()
        .statusCode(HttpStatus.SC_NOT_FOUND)
        .body(MESSAGE_KEY, Matchers.is(MessageKeys.ERROR_CONFIGURATION_NOT_FOUND));

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldUpdateConfiguration() {
    given(configurationRepository.findOne(configurationDto.getId())).willReturn(configuration);

    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .pathParam(ID, configurationDto.getId().toString())
        .body(configurationDto)
        .when()
        .put(ID_URL)
        .then()
        .statusCode(HttpStatus.SC_OK)
        .body(ID, Matchers.is(configurationDto.getId().toString()));

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnNotFoundMessageIfConfigurationDoesNotExistForUpdateConfiguration() {
    given(configurationRepository.findOne(configurationDto.getId())).willReturn(null);

    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .pathParam(ID, configurationDto.getId().toString())
        .body(configurationDto)
        .when()
        .put(ID_URL)
        .then()
        .statusCode(HttpStatus.SC_NOT_FOUND)
        .body(MESSAGE_KEY, is(MessageKeys.ERROR_CONFIGURATION_NOT_FOUND));

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnBadRequestMessageIfConfigurationCannotBeUpdated() {
    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .pathParam(ID, UUID.randomUUID().toString())
        .body(configurationDto)
        .when()
        .put(ID_URL)
        .then()
        .statusCode(HttpStatus.SC_BAD_REQUEST)
        .body(MESSAGE_KEY, is(MessageKeys.ERROR_CONFIGURATION_ID_MISMATCH));

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnUnauthorizedForUpdateConfigurationEndpointIfUserIsNotAuthorized() {
    restAssured
        .given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .pathParam(ID, configurationDto.getId().toString())
        .body(configurationDto)
        .when()
        .put(ID_URL)
        .then()
        .statusCode(HttpStatus.SC_UNAUTHORIZED);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

}