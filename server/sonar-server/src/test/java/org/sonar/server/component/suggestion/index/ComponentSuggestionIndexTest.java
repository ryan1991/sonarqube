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

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.Paging;
import org.sonar.db.component.ComponentQuery;
import org.sonar.server.es.EsTester;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentSuggestionIndexTest {

  @Rule
  public EsTester es = new EsTester(new ComponentSuggestionIndexDefinition(new MapSettings()));

  private ComponentSuggestionIndex underTest = new ComponentSuggestionIndex(es.client());

  @Test
  public void empty_search() {
    ComponentQuery query = ComponentQuery.builder().setNameOrKeyQuery("").setQualifiers(Qualifiers.PROJECT).build();

    List<String> result = underTest.search(query, Paging.forPageIndex(1).withPageSize(6).andTotal(0));

    assertThat(result).isEmpty();
  }
}
