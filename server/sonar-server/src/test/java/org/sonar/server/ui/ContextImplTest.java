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

package org.sonar.server.ui;

import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.web.page.Page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.utils.log.LoggerLevel.WARN;

public class ContextImplTest {

  @Rule
  public LogTester LOGGER = new LogTester();

  private ContextImpl underTest = new ContextImpl();

  private Page page = Page.builder("project_export", "Project Export", "/static/governance/project_export.js").build();

  @Test
  public void no_pages_with_the_same_path() {
    underTest.addPage(page);
    assertThat(LOGGER.logs()).isEmpty();
    underTest.addPage(page);

    Collection<Page> result = underTest.getPages();

    assertThat(result).hasSize(1);
    assertThat(LOGGER.logs(WARN)).contains("Page 'Project Export' cannot be loaded. Another page with path '/static/governance/project_export.js' already exists.");
  }

  @Test
  public void ordered_by_name() {
    underTest
      .addPage(Page.builder("K2", "N2", "P2").build())
      .addPage(Page.builder("K3", "N3", "P3").build())
      .addPage(Page.builder("K1", "N1", "P1").build());

    Collection<Page> result = underTest.getPages();

    assertThat(result).extracting(Page::getKey, Page::getName, Page::getPath)
      .containsOnly(
        tuple("K1", "N1", "P1"),
        tuple("K2", "N2", "P2"),
        tuple("K3", "N3", "P3"));
  }

  @Test
  public void empty_pages_by_default() {
    Collection<Page> result = underTest.getPages();

    assertThat(result).isEmpty();
  }

}
