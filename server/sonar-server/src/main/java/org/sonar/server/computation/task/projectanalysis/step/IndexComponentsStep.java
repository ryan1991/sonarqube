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
package org.sonar.server.computation.task.projectanalysis.step;

import org.sonar.db.component.ResourceIndexDao;
import org.sonar.server.component.suggestion.index.ComponentSuggestionIndexer;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.step.ComputationStep;

/**
 * Components are currently indexed in db table RESOURCE_INDEX, not in Elasticsearch
 */
public class IndexComponentsStep implements ComputationStep {

  private final ResourceIndexDao resourceIndexDao;
  private ComponentSuggestionIndexer elasticSearchIndexer;
  private final TreeRootHolder treeRootHolder;

  public IndexComponentsStep(ResourceIndexDao resourceIndexDao, ComponentSuggestionIndexer elasticSearchIndexer, TreeRootHolder treeRootHolder) {
    this.resourceIndexDao = resourceIndexDao;
    this.elasticSearchIndexer = elasticSearchIndexer;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public void execute() {
    String projectUuid = treeRootHolder.getRoot().getUuid();
    resourceIndexDao.indexProject(projectUuid);
    elasticSearchIndexer.index(projectUuid);
  }

  @Override
  public String getDescription() {
    return "Index components";
  }
}
