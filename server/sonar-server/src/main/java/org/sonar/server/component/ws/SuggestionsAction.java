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
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.suggestion.index.ComponentSuggestionIndex;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsComponents.SearchWsResponse;

import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SUGGESTIONS;

public class SuggestionsAction implements ComponentsWsAction {

  private static final String URL_PARAM_QUERY = "s";
  
  private final DbClient dbClient;
  private final ComponentSuggestionIndex index;

  public SuggestionsAction(DbClient dbClient, ComponentSuggestionIndex index) {
    this.dbClient = dbClient;
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
    SearchWsResponse searchWsResponse = doHandle(wsRequest.param(URL_PARAM_QUERY));
    writeProtobuf(searchWsResponse, wsRequest, wsResponse);
  }

  private SearchWsResponse doHandle(String query) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      List<Component> components = searchComponents(dbSession, query);
      return buildResponse(components);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private List<Component> searchComponents(DbSession dbSession, String query) {
    List<String> uuids = index.search(query);
    List<ComponentDto> dtos = dbClient.componentDao().selectByUuids(dbSession, uuids);
    return dtos.stream().map(SuggestionsAction::dtoToResponse).collect(Collectors.toList());
  }

  private static SearchWsResponse buildResponse(List<Component> components) {
    return SearchWsResponse.newBuilder()
      .addAllComponents(components)
      .build();
  }

  public static WsComponents.Component dtoToResponse(@Nonnull ComponentDto dto) {
    WsComponents.Component.Builder builder = WsComponents.Component.newBuilder()
      .setId(dto.uuid())
      .setKey(dto.key())
      .setName(dto.name())
      .setQualifier(dto.qualifier());
    return builder.build();
  }
}
