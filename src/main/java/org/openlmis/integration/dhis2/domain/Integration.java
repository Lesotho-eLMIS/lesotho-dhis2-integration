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

package org.openlmis.integration.dhis2.domain;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.javers.core.metamodel.annotation.TypeName;

@Entity
@Table(name = "integrations")
@TypeName("Integration")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Integration extends BaseEntity {

  @Column(nullable = false, unique = true, columnDefinition = TEXT_COLUMN_DEFINITION)
  private String name;

  @Getter(AccessLevel.PACKAGE)
  @Type(type = UUID_TYPE)
  @Column(nullable = false, unique = true)
  private UUID programId;

  @Column(nullable = false, columnDefinition = TEXT_COLUMN_DEFINITION)
  private String cronExpression;

  @Setter
  @ManyToOne
  @JoinColumn(name = "configurationId", nullable = false)
  private Configuration configuration;

  String getTargetUrl() {
    return configuration.getTargetUrl();
  }

  /**
   * Update this from another.
   */
  public void updateFrom(Importer importer) {
    this.name = importer.getName();
    this.programId = importer.getProgramId();
    this.cronExpression = importer.getCronExpression();
  }

  /**
   * Export the current object state.
   */
  public void export(Exporter exporter) {
    exporter.setId(getId());
    exporter.setName(name);
    exporter.setProgramId(programId);
    exporter.setCronExpression(cronExpression);
    exporter.setConfiguration(configuration);
  }

  public interface Importer extends BaseImporter {

    String getName();

    UUID getProgramId();

    String getCronExpression();

  }

  public interface Exporter extends BaseExporter {

    void setName(String name);

    void setProgramId(UUID programId);

    void setCronExpression(String cronExpression);

    void setConfiguration(Configuration configuration);
  }

}