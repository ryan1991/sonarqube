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
package org.sonar.server.component.index;

import java.util.List;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.BaseIndex;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.SearchIdResult;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_KEY;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_NAME;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_QUALIFIER;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_UUID;
import static org.sonar.server.component.index.ComponentIndexDefinition.INDEX_COMPONENTS;
import static org.sonar.server.component.index.ComponentIndexDefinition.TYPE_COMPONENT;

public class ComponentIndex extends BaseIndex {

  private DbClient dbClient;

  public ComponentIndex(DbClient dbClient, EsClient client) {
    super(client);
    this.dbClient = dbClient;
  }

  public List<ComponentDto> search(ComponentIndexQuery query) {// TODO test
    DbSession dbSession = dbClient.openSession(false);
    try {
      return dbClient.componentDao().selectByUuids(dbSession, searchIds(query));
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  public List<String> searchIds(ComponentIndexQuery query) {
    System.out.println("search " + query.getQualifier());
    SearchRequestBuilder requestBuilder = getClient()
      .prepareSearch(INDEX_COMPONENTS)
      .setTypes(TYPE_COMPONENT)
      .setFetchSource(false)
      .addField(FIELD_UUID)
      .addSort(FIELD_NAME + "." + SORT_SUFFIX, SortOrder.ASC);

    query.getLimit().ifPresent(requestBuilder::setSize);

    requestBuilder.setQuery(
      createQuery(query));

    List<String> ids = new SearchIdResult<>(requestBuilder.get(), id -> id).getIds();
    System.out.println("  found " + ids);
    return ids;
  }

  private static QueryBuilder createQuery(ComponentIndexQuery query) {
    return boolQuery()
      .filter(termQuery(FIELD_QUALIFIER, query.getQualifier()))
      .filter(boolQuery()
        .should(matchQuery(FIELD_NAME, query))
        .should(termQuery(FIELD_KEY, query))
        .minimumNumberShouldMatch(1));
  }
}
