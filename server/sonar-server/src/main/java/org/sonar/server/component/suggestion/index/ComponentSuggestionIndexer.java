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
//import org.sonar.db.component.ComponentSuggestionIndexerIterator.ComponentSuggestion;
import org.sonar.server.es.BaseIndexer;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;

import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.FIELD_CREATED_AT;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.INDEX_COMPONENT_SUGGESTION;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.TYPE_COMPONENT_SUGGESTION;

public class ComponentSuggestionIndexer extends BaseIndexer {

  public ComponentSuggestionIndexer(System2 system2, EsClient esClient) {
    super(system2, esClient, 300, INDEX_COMPONENT_SUGGESTION, TYPE_COMPONENT_SUGGESTION, FIELD_CREATED_AT);
  }

  @Override
  protected long doIndex(long lastUpdatedAt) {
    return 0;// FIXME implement
  }

  public void index(ComponentSuggestionDoc doc) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_COMPONENT_SUGGESTION);
    bulk.setLarge(false);
    bulk.start();
    bulk.add(newIndexRequest(doc));
    bulk.stop();
  }

  private static IndexRequest newIndexRequest(ComponentSuggestionDoc doc) {
    return new IndexRequest(INDEX_COMPONENT_SUGGESTION, TYPE_COMPONENT_SUGGESTION, doc.getId())
      .source(doc.getFields());
  }
}
