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

import java.util.HashMap;
import org.sonar.server.es.BaseDoc;

public class ComponentSuggestionDoc extends BaseDoc {

  public ComponentSuggestionDoc() {
    super(new HashMap<>(3));
  }

  @Override
  public String getId() {
    return getField(ComponentSuggestionIndexDefinition.FIELD_UUID);
  }

  @Override
  public String getRouting() {
    return getId();
  }

  @Override
  public String getParent() {
    return getId();
  }

  public ComponentSuggestionDoc setId(String s) {
    setField(ComponentSuggestionIndexDefinition.FIELD_UUID, s);
    return this;
  }

  public String getName() {
    return getField(ComponentSuggestionIndexDefinition.FIELD_NAME);
  }

  public ComponentSuggestionDoc setName(String s) {
    setField(ComponentSuggestionIndexDefinition.FIELD_NAME, s);
    return this;
  }

  public String getQualifier() {
    return getField(ComponentSuggestionIndexDefinition.FIELD_QUALIFIER);
  }

  public ComponentSuggestionDoc setQualifier(String s) {
    setField(ComponentSuggestionIndexDefinition.FIELD_QUALIFIER, s);
    return this;
  }
}
