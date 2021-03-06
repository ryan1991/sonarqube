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
package org.sonar.scanner.phases;

import com.google.common.collect.Lists;
import java.util.Collection;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.bootstrap.ScannerExtensionDictionnary;
import org.sonar.scanner.events.EventBus;
import org.sonar.scanner.util.BatchUtils;

@ScannerSide
public class PostJobsExecutor {
  private static final Logger LOG = Loggers.get(PostJobsExecutor.class);

  private final ScannerExtensionDictionnary selector;
  private final Project project;
  private final EventBus eventBus;

  public PostJobsExecutor(ScannerExtensionDictionnary selector, Project project, EventBus eventBus) {
    this.selector = selector;
    this.project = project;
    this.eventBus = eventBus;
  }

  public void execute(SensorContext context) {
    Collection<PostJob> postJobs = selector.select(PostJob.class, project, true, null);

    eventBus.fireEvent(new PostJobPhaseEvent(Lists.newArrayList(postJobs), true));
    execute(context, postJobs);
    eventBus.fireEvent(new PostJobPhaseEvent(Lists.newArrayList(postJobs), false));
  }

  private void execute(SensorContext context, Collection<PostJob> postJobs) {
    logPostJobs(postJobs);

    for (PostJob postJob : postJobs) {
      LOG.info("Executing post-job {}", BatchUtils.describe(postJob));
      eventBus.fireEvent(new PostJobExecutionEvent(postJob, true));
      postJob.executeOn(project, context);
      eventBus.fireEvent(new PostJobExecutionEvent(postJob, false));
    }
  }

  private static void logPostJobs(Collection<PostJob> postJobs) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(() -> "Post-jobs : " + StringUtils.join(postJobs, " -> "));
    }
  }
}
