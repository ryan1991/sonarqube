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
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.web.page.Context;
import org.sonar.api.web.page.Page;

class ContextImpl implements Context {
  private static final Logger LOGGER = Loggers.get(ContextImpl.class);

  private Map<String, Page> pagesByPath = new HashMap<>();

  @Override
  public Context addPage(Page page) {
    Page existingPageWithSamePath = pagesByPath.putIfAbsent(page.getPath(), page);
    if (existingPageWithSamePath != null) {
      LOGGER.warn("Page '{}' cannot be loaded. Another page with path '{}' already exists.", page.getName(), page.getPath());
    }

    return this;
  }

  public Collection<Page> getPages() {
    return pagesByPath.values();
  }
}
