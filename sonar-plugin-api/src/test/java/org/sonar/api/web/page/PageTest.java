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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.web.page.Page.Qualifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.page.Page.Navigation.COMPONENT_ADMIN;
import static org.sonar.api.web.page.Page.Navigation.GLOBAL;
import static org.sonar.api.web.page.Page.Qualifier.MODULE;
import static org.sonar.api.web.page.Page.Qualifier.PROJECT;
import static org.sonar.api.web.page.Page.Qualifier.SUB_VIEW;
import static org.sonar.api.web.page.Page.Qualifier.VIEW;

public class PageTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Page.Builder underTest = Page.builder("project_dump", "Project Dump", "/static/governance/project_dump.js");

  @Test
  public void full_test() {
    Page result = underTest
      .setQualifiers(PROJECT, MODULE)
      .setNavigation(COMPONENT_ADMIN)
      .build();

    assertThat(result.getQualifiers()).containsExactly(PROJECT, MODULE);
    assertThat(result.getNavigation()).isEqualTo(COMPONENT_ADMIN);
  }

  @Test
  public void qualifiers_map_to_key() {
    assertThat(Qualifier.PROJECT.getKey()).isEqualTo(org.sonar.api.resources.Qualifiers.PROJECT);
    assertThat(Qualifier.MODULE.getKey()).isEqualTo(org.sonar.api.resources.Qualifiers.MODULE);
    assertThat(Qualifier.VIEW.getKey()).isEqualTo(org.sonar.api.resources.Qualifiers.VIEW);
    assertThat(Qualifier.SUB_VIEW.getKey()).isEqualTo(org.sonar.api.resources.Qualifiers.SUBVIEW);
  }

  @Test
  public void authorized_qualifiers() {
    Qualifier[] qualifiers = Qualifier.values();

    assertThat(qualifiers).hasSize(4).containsOnly(PROJECT, MODULE, VIEW, SUB_VIEW);
  }

  @Test
  public void default_values() {
    Page result = underTest.build();

    assertThat(result.getQualifiers()).containsOnly(Qualifier.values());
    assertThat(result.getNavigation()).isEqualTo(GLOBAL);
  }

  @Test
  public void fail_if_no_qualifier() {
    expectedException.expect(IllegalArgumentException.class);

    underTest.setQualifiers().build();
  }

  @Test
  public void fail_if_a_page_has_no_key() {
    expectedException.expect(IllegalArgumentException.class);

    Page.builder(null, "Project Dump", "/static/governance/project_dump.js").build();
  }

  @Test
  public void fail_if_a_page_has_no_name() {
    expectedException.expect(IllegalArgumentException.class);

    Page.builder("project_dump", null, "/static/governance/project_dump.js").build();
  }

  @Test
  public void fail_if_page_has_no_path() {
    expectedException.expect(IllegalArgumentException.class);

    Page.builder("project_dump", "Project Dump", null).build();
  }
}
