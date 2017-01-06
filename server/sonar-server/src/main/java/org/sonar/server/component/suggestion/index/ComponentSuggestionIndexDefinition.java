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

import org.sonar.api.config.Settings;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.NewIndex;

public class ComponentSuggestionIndexDefinition implements IndexDefinition {

  public static final String INDEX_COMPONENT_SUGGESTION = "componentsuggestions";
  public static final String TYPE_COMPONENT_SUGGESTION = "componentsuggestion";

  public static final String FIELD_UUID = "uuid";
  public static final String FIELD_PROJECT_UUID = "uuid";
  public static final String FIELD_KEY = "key";
  public static final String FIELD_NAME = "name";
  public static final String FIELD_QUALIFIER = "qualifier";

  private static final int DEFAULT_NUMBER_OF_SHARDS = 5;

  private final Settings settings;

  public ComponentSuggestionIndexDefinition(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX_COMPONENT_SUGGESTION);
    index.refreshHandledByIndexer();
    index.configureShards(settings, DEFAULT_NUMBER_OF_SHARDS);

    // type "componentsuggestion"
    NewIndex.NewIndexType mapping = index.createType(TYPE_COMPONENT_SUGGESTION);
    mapping.stringFieldBuilder(FIELD_UUID).disableNorms().build();
    mapping.stringFieldBuilder(FIELD_PROJECT_UUID).build();
    mapping.stringFieldBuilder(FIELD_KEY).build();
    mapping.stringFieldBuilder(FIELD_NAME).enableSorting().enableGramSearch().build();
    mapping.stringFieldBuilder(FIELD_QUALIFIER).build();

    // do not store document but only indexation of information
    mapping.setEnableSource(false);
  }
}
