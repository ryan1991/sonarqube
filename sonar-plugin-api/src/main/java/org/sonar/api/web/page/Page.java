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

import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;

import static org.sonar.api.web.page.Page.Navigation.GLOBAL;

/**
 * @since 6.3
 */
public final class Page {
  private final String key;
  private final String name;
  private final String path;
  private final List<Qualifier> qualifiers;
  private final Navigation navigation;

  public Page(Builder builder) {
    this.key = builder.key;
    this.name = builder.name;
    this.path = builder.path;
    this.qualifiers = Arrays.asList(builder.qualifiers);
    this.navigation = builder.navigation;
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  public List<Qualifier> getQualifiers() {
    return qualifiers;
  }

  public Navigation getNavigation() {
    return navigation;
  }

  public static Builder builder(String key, String name, String path) {
    return new Builder(key, name, path);
  }

  public static class Builder {
    private final String key;
    private final String name;
    private final String path;
    private Qualifier[] qualifiers = Qualifier.values();
    private Navigation navigation = GLOBAL;

    private Builder(String key, String name, String path) {
      this.key = key;
      this.name = name;
      this.path = path;
    }

    public Builder setQualifiers(Qualifier... qualifiers) {
      this.qualifiers = qualifiers;
      return this;
    }

    public Builder setNavigation(Navigation navigation) {
      this.navigation = navigation;
      return this;
    }

    public Page build() {
      if (key == null || name == null || path == null) {
        throw new IllegalArgumentException("A Page must have a key, a name and a path");
      }
      if (qualifiers.length == 0) {
        throw new IllegalArgumentException("A Page must have at least one qualifier");
      }
      if (navigation == null) {
        throw new IllegalArgumentException("A Page must have a navigation");
      }

      return new Page(this);
    }
  }

  public enum Qualifier {
    PROJECT(org.sonar.api.resources.Qualifiers.PROJECT),
    MODULE(org.sonar.api.resources.Qualifiers.MODULE),
    VIEW(org.sonar.api.resources.Qualifiers.VIEW),
    SUB_VIEW(org.sonar.api.resources.Qualifiers.SUBVIEW);

    private final String key;

    Qualifier(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }

    @CheckForNull
    public static Qualifier fromKey(String key) {
      for (Qualifier qualifier : values()) {
        if (qualifier.getKey().equals(key)) {
          return qualifier;
        }
      }

      return null;
    }
  }

  public enum Navigation {
    GLOBAL, GLOBAL_ADMIN, COMPONENT, COMPONENT_ADMIN
  }
}
