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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.Startable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.page.Page;
import org.sonar.api.web.page.PagesDefinition;
import org.sonar.core.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;

@ServerSide
public class PageRepository implements Startable {
  private final List<PagesDefinition> definitions;
  private List<Page> pages;

  public PageRepository() {
    // in case there's no page definition
    definitions = Collections.emptyList();
  }

  public PageRepository(PagesDefinition[] pagesDefinitions) {
    definitions = ImmutableList.copyOf(pagesDefinitions);
  }

  @Override
  public void start() {
    ContextImpl context = new ContextImpl();
    definitions.forEach(definition -> definition.define(context));
    pages = ImmutableList.copyOf(context.getPages().stream()
      .sorted(comparing(Page::getPath))
      .collect(Collectors.toList()));
  }

  @Override
  public void stop() {
    // nothing to do
  }

  @VisibleForTesting
  List<Page> getAllPages() {
    return requireNonNull(pages, "Pages haven't been initialized yet");
  }

  public List<Page> getPages(Page.Navigation navigation, @Nullable Page.Qualifier qualifier) {
    return pages.stream()
      .filter(page -> navigation.equals(page.getNavigation()))
      .filter(page -> page.getQualifiers().contains(qualifier))
      .collect(Collectors.toList());
  }

}
