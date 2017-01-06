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

package org.sonar.api.web.page;

import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FakeContextTest {
  FakeContext underTest = new FakeContext();

  @Test
  public void add_pages_and_retrieve_them() {
    underTest
      .addPage(Page.builder("K1", "N1", "P1").build())
      .addPage(Page.builder("K2", "N2", "P2").build());

    List<Page> result = underTest.getPages();

    assertThat(result).hasSize(2).extracting(Page::getKey).containsExactly("K1", "K2");
  }
}
