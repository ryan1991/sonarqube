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
package org.sonar.scanner.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.PluginInfo;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;
import org.sonar.scanner.protocol.input.GlobalRepositories;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.repository.ProjectRepositories;

@ScannerSide
public class AnalysisContextReportPublisher {

  private static final String KEY_VALUE_FORMAT = "  - %s=%s";

  private static final Logger LOG = Loggers.get(AnalysisContextReportPublisher.class);

  private static final String ENV_PROP_PREFIX = "env.";
  private static final String SONAR_PROP_PREFIX = "sonar.";
  private final ScannerPluginRepository pluginRepo;
  private final AnalysisMode mode;
  private final System2 system;
  private final ProjectRepositories projectRepos;
  private final GlobalRepositories globalRepositories;

  private ScannerReportWriter writer;

  public AnalysisContextReportPublisher(AnalysisMode mode, ScannerPluginRepository pluginRepo, System2 system,
    ProjectRepositories projectRepos, GlobalRepositories globalRepositories) {
    this.mode = mode;
    this.pluginRepo = pluginRepo;
    this.system = system;
    this.projectRepos = projectRepos;
    this.globalRepositories = globalRepositories;
  }

  public void init(ScannerReportWriter writer) {
    if (mode.isIssues()) {
      return;
    }
    this.writer = writer;
    File analysisLog = writer.getFileStructure().analysisLog();
    try (BufferedWriter fileWriter = Files.newBufferedWriter(analysisLog.toPath(), StandardCharsets.UTF_8)) {
      if (LOG.isDebugEnabled()) {
        writeEnvVariables(fileWriter);
        writeSystemProps(fileWriter);
      }
      writePlugins(fileWriter);
      writeGlobalSettings(fileWriter);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write analysis log", e);
    }
  }

  private void writePlugins(BufferedWriter fileWriter) throws IOException {
    fileWriter.write("SonarQube plugins:\n");
    for (PluginInfo p : pluginRepo.getPluginInfos()) {
      fileWriter.append(String.format("  - %s %s (%s)", p.getName(), p.getVersion(), p.getKey())).append('\n');
    }
  }

  private void writeSystemProps(BufferedWriter fileWriter) throws IOException {
    fileWriter.write("System properties:\n");
    Properties sysProps = system.properties();
    for (String prop : new TreeSet<>(sysProps.stringPropertyNames())) {
      if (prop.startsWith(SONAR_PROP_PREFIX)) {
        continue;
      }
      fileWriter.append(String.format(KEY_VALUE_FORMAT, prop, sysProps.getProperty(prop))).append('\n');
    }
  }

  private void writeEnvVariables(BufferedWriter fileWriter) throws IOException {
    fileWriter.append("Environment variables:\n");
    Map<String, String> envVariables = system.envVariables();
    for (String env : new TreeSet<>(envVariables.keySet())) {
      fileWriter.append(String.format(KEY_VALUE_FORMAT, env, envVariables.get(env))).append('\n');
    }
  }

  private void writeGlobalSettings(BufferedWriter fileWriter) throws IOException {
    fileWriter.append("Global properties:\n");
    Map<String, String> props = globalRepositories.globalSettings();
    for (String prop : new TreeSet<>(props.keySet())) {
      dumpPropIfNotSensitive(fileWriter, prop, props.get(prop));
    }
  }

  public void dumpModuleSettings(ProjectDefinition moduleDefinition) {
    if (mode.isIssues()) {
      return;
    }

    File analysisLog = writer.getFileStructure().analysisLog();
    try (BufferedWriter fileWriter = Files.newBufferedWriter(analysisLog.toPath(), StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
      Map<String, String> moduleSpecificProps = collectModuleSpecificProps(moduleDefinition);
      fileWriter.append(String.format("Settings for module: %s", moduleDefinition.getKey())).append('\n');
      for (String prop : new TreeSet<>(moduleSpecificProps.keySet())) {
        if (isSystemProp(prop) || isEnvVariable(prop) || !isSqProp(prop)) {
          continue;
        }
        dumpPropIfNotSensitive(fileWriter, prop, moduleSpecificProps.get(prop));
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write analysis log", e);
    }
  }

  private static void dumpPropIfNotSensitive(BufferedWriter fileWriter, String prop, String value) throws IOException {
    fileWriter.append(String.format(KEY_VALUE_FORMAT, prop, sensitive(prop) ? "******" : value)).append('\n');
  }

  /**
   * Only keep props that are not in parent
   */
  private Map<String, String> collectModuleSpecificProps(ProjectDefinition moduleDefinition) {
    Map<String, String> moduleSpecificProps = new HashMap<>();
    if (projectRepos.moduleExists(moduleDefinition.getKeyWithBranch())) {
      moduleSpecificProps.putAll(projectRepos.settings(moduleDefinition.getKeyWithBranch()));
    }
    ProjectDefinition parent = moduleDefinition.getParent();
    if (parent == null) {
      moduleSpecificProps.putAll(moduleDefinition.properties());
    } else {
      Map<String, String> parentProps = parent.properties();
      for (Map.Entry<String, String> entry : moduleDefinition.properties().entrySet()) {
        if (!parentProps.containsKey(entry.getKey()) || !parentProps.get(entry.getKey()).equals(entry.getValue())) {
          moduleSpecificProps.put(entry.getKey(), entry.getValue());
        }
      }
    }
    return moduleSpecificProps;
  }

  private static boolean isSqProp(String propKey) {
    return propKey.startsWith(SONAR_PROP_PREFIX);
  }

  private boolean isSystemProp(String propKey) {
    return system.properties().containsKey(propKey) && !propKey.startsWith(SONAR_PROP_PREFIX);
  }

  private boolean isEnvVariable(String propKey) {
    return propKey.startsWith(ENV_PROP_PREFIX) && system.envVariables().containsKey(propKey.substring(ENV_PROP_PREFIX.length()));
  }

  private static boolean sensitive(String key) {
    return key.equals(CoreProperties.LOGIN) || key.contains(".password") || key.contains(".secured");
  }
}
