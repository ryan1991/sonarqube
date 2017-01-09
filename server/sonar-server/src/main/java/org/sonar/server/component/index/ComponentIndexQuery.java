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
package org.sonar.server.component.index;

import java.util.Optional;

public class ComponentIndexQuery {

  private final String qualifier;
  private final String query;
  private final Optional<Integer> limit;

  public ComponentIndexQuery(String qualifier, String query) {
    this.qualifier = qualifier;
    this.query = query;
    this.limit = Optional.empty();
  }

  public ComponentIndexQuery(String qualifier, String query, Integer limit) {
    this.qualifier = qualifier;
    this.query = query;
    this.limit = Optional.of(limit);
  }

  public String getQualifier() {
    return qualifier;
  }

  public String getQuery() {
    return query;
  }

  public Optional<Integer> getLimit() {
    return limit;
  }

  // TODO clarify, if serialversionuid, hashcode, etc. should be implemented

}
