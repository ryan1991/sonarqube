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

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Test;
import org.sonar.core.util.stream.Collectors;
import org.sonarqube.ws.WsComponents.Component;
import org.sonarqube.ws.WsComponents.SuggestionsWsResponse.Qualifier;

import static org.assertj.core.api.Assertions.assertThat;

public class SuggestionsActionTest {

  @Test
  public void calculate_total_without_qualifier() {
    assertTotal(0);
  }

  @Test
  public void calculate_total_of_single_qualifier() {
    assertTotal(42, 42);
  }

  @Test
  public void calculate_total_of_several_qualifiers() {
    assertTotal(42, 20, 20, 2);
  }

  private void assertTotal(int total, int... numbers) {
    assertThat(SuggestionsAction.getTotal(createQualifiers(42))).isEqualTo(42);
  }

  private List<Qualifier> createQualifiers(int... numbersOfResults) {
    return Arrays
      .stream(numbersOfResults)
      .mapToObj(this::createResults)
      .map(results -> Qualifier.newBuilder().addAllResults(results).build())
      .collect(Collectors.toList());
  }

  private Iterable<Component> createResults(int numberOfResults) {
    return IntStream
      .range(0, numberOfResults)
      .mapToObj(i -> Component.newBuilder().build()).collect(Collectors.toList());
  }
}
