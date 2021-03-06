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
package org.sonar.scanner.scan.filesystem;

import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.scanner.index.BatchComponent;
import org.sonar.scanner.index.BatchComponentCache;
import org.sonar.scanner.index.DefaultIndex;

/**
 * Index all files/directories of the module in SQ database and importing source code.
 *
 * @since 4.2
 */
@ScannerSide
public class ComponentIndexer {

  private final Languages languages;
  private final DefaultIndex sonarIndex;
  private final Project module;
  private final BatchComponentCache componentCache;

  public ComponentIndexer(Project module, Languages languages, DefaultIndex sonarIndex, BatchComponentCache componentCache) {
    this.module = module;
    this.languages = languages;
    this.sonarIndex = sonarIndex;
    this.componentCache = componentCache;
  }

  public void execute(DefaultModuleFileSystem fs) {
    module.setBaseDir(fs.baseDir());

    for (InputFile inputFile : fs.inputFiles()) {
      String languageKey = inputFile.language();
      boolean unitTest = InputFile.Type.TEST == inputFile.type();
      Resource sonarFile = File.create(inputFile.relativePath(), languages.get(languageKey), unitTest);
      sonarIndex.index(sonarFile);
      BatchComponent file = componentCache.get(sonarFile);
      file.setInputComponent(inputFile);
      Resource sonarDir = file.parent().resource();
      InputDir inputDir = fs.inputDir(inputFile.file().getParentFile());
      componentCache.get(sonarDir).setInputComponent(inputDir);
    }
  }
}
