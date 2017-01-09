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
package org.sonar.server.component.ws;

import com.google.common.io.Resources;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.component.index.ComponentIndexQuery;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse.Qualifier;

import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SUGGESTIONS;

public class SuggestionsAction implements ComponentsWsAction {

  private static final String URL_PARAM_QUERY = "s";

  private static final String[] QUALIFIERS = {
    Qualifiers.VIEW,
    Qualifiers.SUBVIEW,
    Qualifiers.PROJECT,
    Qualifiers.LIBRARY, // TODO clarify if necessary
    Qualifiers.MODULE,
    Qualifiers.DIRECTORY, // TODO clarify if necessary
    Qualifiers.FILE,
    Qualifiers.UNIT_TEST_FILE
  };
  private static final int NUMBER_OF_RESULTS_PER_QUALIFIER = 6;

  private final ComponentIndex index;

  public SuggestionsAction(ComponentIndex index) {
    this.index = index;
  }

  @Override
  public void define(WebService.NewController context) {
    NewAction action = context.createAction(ACTION_SUGGESTIONS)
      .setDescription("Internal WS for the top-right search engine")
      .setSince("4.2")
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(Resources.getResource(this.getClass(), "components-example-suggestions.json"));

    action.createParam(URL_PARAM_QUERY)
      .setRequired(true)
      .setDescription("Substring of project key (minimum 2 characters)")
      .setExampleValue("sonar");

    RailsHandler.addJsonOnlyFormatParam(action);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    SuggestionsWsResponse searchWsResponse = doHandle(wsRequest.param(URL_PARAM_QUERY));
    writeProtobuf(searchWsResponse, wsRequest, wsResponse);
  }

  private SuggestionsWsResponse doHandle(String query) {
    List<Qualifier> resultsPerQualifier = getResultsOfAllQualifiers(query);

    return SuggestionsWsResponse.newBuilder()
      .setTotal(getTotal(resultsPerQualifier))
      .addAllQualifiers(resultsPerQualifier)
      .build();
  }

  static int getTotal(List<Qualifier> resultsPerQualifier) {
    return resultsPerQualifier.stream().mapToInt(Qualifier::getResultsCount).sum();
  }

  private List<Qualifier> getResultsOfAllQualifiers(String query) {
    return Arrays
      .stream(QUALIFIERS)
      .map(qualifier -> getResultsOfQualifier(query, qualifier))
      .collect(Collectors.toList());
  }

  private Qualifier getResultsOfQualifier(String query, String qualifier) {
    List<Component> results = index
      .search(new ComponentIndexQuery(qualifier, query, NUMBER_OF_RESULTS_PER_QUALIFIER))
      .stream()
      .map(SuggestionsAction::dtoToComponent)
      .collect(Collectors.toList());

    return Qualifier.newBuilder()
      .addAllResults(results)
      .build();
  }

  private static Component dtoToComponent(ComponentDto result) {
    return Component.newBuilder()
      .setId(Long.toString(result.getId()))
      .setName(result.name())
      .build();
  }

}
