/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.component.suggestion.index;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.EsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.INDEX_COMPONENT_SUGGESTION;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.TYPE_COMPONENT_SUGGESTION;

public class ComponentSuggestionIndexerTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester esTester = new EsTester(new ComponentSuggestionIndexDefinition(new MapSettings()));

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();

  @Test
  public void index_nothing() {
    assertThat(indexAndReturnCount()).isZero();
  }

  @Test
  public void index() {
    ComponentDto component = new ComponentDto()
      .setKey("KEY-1")
      .setUuid("UUID-1")
      .setRootUuid("ROOT-1")
      .setUuidPath("PATH-1")
      .setQualifier(Qualifiers.PROJECT)
      .setName("Name")
      .setDescription("Description");
    dbClient.componentDao().insert(dbSession, component);
    dbSession.commit();

    assertThat(indexAndReturnCount()).isEqualTo(1);
  }

  @Test
  public void index_one_project_have_document_of_another() {
    ComponentDto component = new ComponentDto()
      .setKey("KEY-1")
      .setUuid("UUID-1")
      .setRootUuid("ROOT-1")
      .setUuidPath("PATH-1")
      .setQualifier(Qualifiers.PROJECT)
      .setName("Name")
      .setDescription("Description");
    dbClient.componentDao().insert(dbSession, component);
    dbSession.commit();

    ComponentSuggestionIndexer indexer = createIndexer();
    indexer.index("UUID-2");
    long countDocuments = esTester.countDocuments(INDEX_COMPONENT_SUGGESTION, TYPE_COMPONENT_SUGGESTION);

    assertThat(countDocuments).isEqualTo(0);
  }

  @Test
  public void index_one_project_consisting_of_single_component() {
    ComponentDto component = new ComponentDto()
      .setKey("KEY-1")
      .setUuid("UUID-1")
      .setRootUuid("ROOT-1")
      .setUuidPath("PATH-1")
      .setQualifier(Qualifiers.PROJECT)
      .setName("Name")
      .setDescription("Description");
    dbClient.componentDao().insert(dbSession, component);
    dbSession.commit();

    ComponentSuggestionIndexer indexer = createIndexer();
    indexer.index("UUID-1");
    long countDocuments = esTester.countDocuments(INDEX_COMPONENT_SUGGESTION, TYPE_COMPONENT_SUGGESTION);

    assertThat(countDocuments).isEqualTo(0);
  }

  @Test
  public void index_one_project_containing_a_file() {
    ComponentDto projectComponent = new ComponentDto()
      .setKey("KEY-PROJECT-1")
      .setUuid("UUID-PROJECT-1")
      .setProjectUuid("UUID-PROJECT-1")
      .setRootUuid("UUID-PROJECT-1")
      .setUuidPath("PATH-PROJECT-1")
      .setQualifier(Qualifiers.PROJECT)
      .setName("Project")
      .setDescription("Project description");
    dbClient.componentDao().insert(dbSession, projectComponent);

    ComponentDto fileComponent = new ComponentDto()
      .setKey("KEY-FILE-1")
      .setUuid("UUID-FILE-1")
      .setProjectUuid("UUID-PROJECT-1")// reference to project
      .setRootUuid("UUID-PROJECT-1")
      .setUuidPath("PATH-FILE-1")
      .setQualifier(Qualifiers.FILE)
      .setName("File")
      .setDescription("File description");
    dbClient.componentDao().insert(dbSession, fileComponent);

    dbSession.commit();

    ComponentSuggestionIndexer indexer = createIndexer();
    indexer.index("UUID-PROJECT-1");
    long countDocuments = esTester.countDocuments(INDEX_COMPONENT_SUGGESTION, TYPE_COMPONENT_SUGGESTION);

    assertThat(countDocuments).isEqualTo(2);
  }

  private long indexAndReturnCount() {
    ComponentSuggestionIndexer indexer = createIndexer();
    indexer.index();
    long countDocuments = esTester.countDocuments(INDEX_COMPONENT_SUGGESTION, TYPE_COMPONENT_SUGGESTION);
    return countDocuments;
  }

  private ComponentSuggestionIndexer createIndexer() {
    return new ComponentSuggestionIndexer(dbTester.getDbClient(), esTester.client());
  }

}
