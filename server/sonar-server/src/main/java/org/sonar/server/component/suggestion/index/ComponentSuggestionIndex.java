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
import org.sonar.api.resources.Qualifiers;
import org.sonar.server.es.BaseIndex;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.SearchIdResult;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.FIELD_KEY;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.FIELD_NAME;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.FIELD_QUALIFIER;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.FIELD_UUID;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.INDEX_COMPONENT_SUGGESTION;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.TYPE_COMPONENT_SUGGESTION;

public class ComponentSuggestionIndex extends BaseIndex {

  static final String[] QUALIFIERS = {
    Qualifiers.VIEW,
    Qualifiers.SUBVIEW,
    Qualifiers.PROJECT,
    Qualifiers.LIBRARY, // TODO clarify if necessary
    Qualifiers.MODULE,
    Qualifiers.DIRECTORY, // TODO clarify if necessary
    Qualifiers.FILE,
    Qualifiers.UNIT_TEST_FILE
  };

  public ComponentSuggestionIndex(EsClient client) {
    super(client);
  }

  public List<String> search(String query) {
    return Arrays.stream(QUALIFIERS)
      .flatMap(qualifier -> search(qualifier, query))
      .collect(Collectors.toList());
  }

  private Stream<String> search(String qualifier, String query) {
    System.out.println("search " + qualifier);
    SearchRequestBuilder requestBuilder = getClient()
      .prepareSearch(INDEX_COMPONENT_SUGGESTION)
      .setTypes(TYPE_COMPONENT_SUGGESTION)
      .setFetchSource(false)
      .setSize(6)
      .addField(FIELD_UUID)
      .addSort(FIELD_NAME + "." + SORT_SUFFIX, SortOrder.ASC);

    requestBuilder.setQuery(
      createQuery(qualifier, query));

    List<String> ids = new SearchIdResult<>(requestBuilder.get(), id -> id).getIds();
    System.out.println("  found " + ids);
    return ids.stream();
  }

  private BoolQueryBuilder createQuery(String qualifier, String query) {
    return boolQuery()
      .filter(termQuery(FIELD_QUALIFIER, qualifier))
      .filter(boolQuery()
        .should(wildcardQuery(FIELD_NAME, "*" + query + "*"))//FIXME do not hand user input directly to elasticsearch!
        .should(termQuery(FIELD_KEY, query))
        .minimumNumberShouldMatch(1));
  }
}
