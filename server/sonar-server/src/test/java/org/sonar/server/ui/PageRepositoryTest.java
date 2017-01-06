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

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.web.page.Page;
import org.sonar.api.web.page.Page.Navigation;
import org.sonar.api.web.page.Page.Qualifier;
import org.sonar.api.web.page.PagesDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PageRepositoryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PageRepository underTest = new PageRepository();

  @Test
  public void pages_from_different_page_definitions_ordered_by_path() {
    PagesDefinition firstPlugin = context -> context
      .addPage(Page.builder("K1", "N1", "P1").build())
      .addPage(Page.builder("K3", "N3", "P3").build());
    PagesDefinition secondPlugin = context -> context.addPage(Page.builder("K2", "N2", "P2").build());
    underTest = new PageRepository(new PagesDefinition[] {firstPlugin, secondPlugin});
    underTest.start();

    List<Page> result = underTest.getAllPages();

    assertThat(result).extracting(Page::getKey, Page::getName, Page::getPath)
      .containsExactly(
        tuple("K1", "N1", "P1"),
        tuple("K2", "N2", "P2"),
        tuple("K3", "N3", "P3")
      );
  }

  @Test
  public void filter_by_navigation_and_qualifier() {
    PagesDefinition plugin = context -> context
      // Default with GLOBAL navigation and all qualifiers
      .addPage(Page.builder("K1", "N1", "P1").build())
      .addPage(Page.builder("K2", "N2", "P2").setNavigation(Navigation.COMPONENT).setQualifiers(Qualifier.PROJECT).build())
      .addPage(Page.builder("K3", "N3", "P3").setQualifiers(Qualifier.PROJECT).build())
      .addPage(Page.builder("K4", "N4", "P4").setNavigation(Navigation.GLOBAL).build())
      .addPage(Page.builder("K5", "N5", "P5").setQualifiers(Qualifier.VIEW).setNavigation(Navigation.GLOBAL).build());
    underTest = new PageRepository(new PagesDefinition[]{plugin});
    underTest.start();

    List<Page> result = underTest.getPages(Navigation.GLOBAL, Qualifier.PROJECT);

    assertThat(result).extracting(Page::getKey)
      .containsExactly("K1", "K3", "K4");
  }

  @Test
  public void empty_pages_if_no_page_definition() {
    underTest.start();

    List<Page> result = underTest.getAllPages();

    assertThat(result).isEmpty();
  }

  @Test
  public void fail_if_pages_called_before_server_startup() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Pages haven't been initialized yet");

    underTest.getAllPages();
  }
}
