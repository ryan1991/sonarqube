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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.utils.Paging;
import org.sonar.db.component.ComponentQuery;
import org.sonar.server.es.BaseIndex;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.SearchIdResult;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.FIELD_KEY;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.FIELD_NAME;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.FIELD_QUALIFIER;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.FIELD_UUID;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.INDEX_COMPONENT_SUGGESTION;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.TYPE_COMPONENT_SUGGESTION;

public class ComponentSuggestionIndex extends BaseIndex {

  public ComponentSuggestionIndex(EsClient client) {
    super(client);
  }

  public List<String> search(ComponentQuery query, Paging paging) {
    return Arrays.stream(query.getQualifiers())
      .flatMap(qualifier -> search(qualifier, query, paging))
      .collect(Collectors.toList());
  }

  private Stream<String> search(String qualifier, ComponentQuery query, Paging paging) {
    SearchRequestBuilder requestBuilder = getClient()
      .prepareSearch(INDEX_COMPONENT_SUGGESTION)
      .setTypes(TYPE_COMPONENT_SUGGESTION)
      .setFetchSource(false)
      .setFrom(paging.offset())
      .setSize(paging.total())
      .addField(FIELD_UUID)
      .addSort(FIELD_NAME + "." + SORT_SUFFIX, SortOrder.ASC);

    requestBuilder.setQuery(
      createQuery(qualifier, query.getNameOrKeyQuery()));

    return new SearchIdResult<>(requestBuilder.get(), id -> id).getIds().stream();
  }

  private BoolQueryBuilder createQuery(String qualifier, String query) {
    return boolQuery()
      .filter(termQuery(FIELD_QUALIFIER, qualifier))
      .filter(boolQuery()
        .should(termQuery(FIELD_NAME, query))
        .should(termQuery(FIELD_KEY, query))
        .minimumNumberShouldMatch(1));
  }
}
