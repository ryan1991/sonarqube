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

import org.elasticsearch.action.index.IndexRequest;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
//import org.sonar.db.component.ComponentSuggestionIndexerIterator.ComponentSuggestion;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;

import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.FIELD_CREATED_AT;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.INDEX_COMPONENT_SUGGESTION;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.TYPE_COMPONENT_SUGGESTION;

public class ComponentSuggestionIndexer extends BaseIndexer {

  private final DbClient dbClient;

  public ComponentSuggestionIndexer(System2 system2, DbClient dbClient, EsClient esClient) {
    super(system2, esClient, 300, INDEX_COMPONENT_SUGGESTION, TYPE_COMPONENT_SUGGESTION, FIELD_CREATED_AT);
    this.dbClient = dbClient;
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    return 0;// FIXME implement
  }

  // @Override
  // protected long doIndex(long lastUpdatedAt) {
  // return doIndex(createBulkIndexer(false), lastUpdatedAt, null);
  // }
  //
  // public void index(String componentUuid) {
  // doIndex(createBulkIndexer(false), 0L, componentUuid);
  // }

  public void deleteProject(String uuid) {
    esClient
      .prepareDelete(INDEX_COMPONENT_SUGGESTION, TYPE_COMPONENT_SUGGESTION, uuid)
      .setRouting(uuid)
      .setRefresh(true)
      .get();
  }

  // private long doIndex(BulkIndexer bulk, long lastUpdatedAt, @Nullable String componentUuid) {
  // // FIXME implement
  // // try (DbSession dbSession = dbClient.openSession(false);
  // // ComponentSuggestionIndexerIterator suggestions = ComponentSuggestionIndexerIterator.create(dbSession, lastUpdatedAt, componentUuid))
  // // {
  // // dbClient.componentDao().selectComponentsByQualifiers(dbSession, qualifiers)
  //
  // Iterator<ComponentSuggestion> suggestions = new ArrayList<ComponentSuggestion>().iterator();
  // return doIndex(bulk, suggestions);
  // // }
  //
  // }

  // private static long doIndex(BulkIndexer bulk, Iterator<ComponentSuggestion> suggestions) {
  // bulk.start();
  // long lastUpdatedAt = 0L;
  // while (suggestions.hasNext()) {
  // ComponentSuggestion suggestion = suggestions.next();
  // bulk.add(newIndexRequest(toDocument(suggestion)));
  //
  // Long updatedAt = suggestion.getCreatedAt();
  // lastUpdatedAt = Math.max(lastUpdatedAt, updatedAt == null ? 0L : updatedAt);
  // }
  // bulk.stop();
  // return lastUpdatedAt;
  // }

  private BulkIndexer createBulkIndexer(boolean large) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_COMPONENT_SUGGESTION);
    bulk.setLarge(large);
    return bulk;
  }

  private static IndexRequest newIndexRequest(ComponentSuggestionDoc doc) {
    String componentUuid = doc.getId();
    return new IndexRequest(INDEX_COMPONENT_SUGGESTION, TYPE_COMPONENT_SUGGESTION, componentUuid)
      .routing(componentUuid)
      .parent(componentUuid)
      .source(doc.getFields());
  }

  // private static ComponentSuggestionDoc toDocument(ComponentSuggestion componentSuggestion) {
  // Long createdAt = componentSuggestion.getCreatedAt();
  // return new ComponentSuggestionDoc()
  // .setId(componentSuggestion.getUuid())
  // .setType(TYPE_COMPONENT_SUGGESTION)
  // .setCreatedAt(createdAt == null ? null : new Date(createdAt)/* TODO put into helper class? */)
  // .setName(componentSuggestion.getName())
  // .setQualifier(componentSuggestion.getQualifier());
  // }
}
