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
package org.sonar.scanner.issue.tracking;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.scanner.analysis.DefaultAnalysisMode;
import org.sonar.scanner.index.BatchComponent;
import org.sonar.scanner.issue.IssueTransformer;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.repository.ProjectRepositories;

@ScannerSide
public class LocalIssueTracking {
  private final Tracker<TrackedIssue, ServerIssueFromWs> tracker;
  private final ServerLineHashesLoader lastLineHashes;
  private final ActiveRules activeRules;
  private final ServerIssueRepository serverIssueRepository;
  private final DefaultAnalysisMode mode;

  private boolean hasServerAnalysis;

  public LocalIssueTracking(Tracker<TrackedIssue, ServerIssueFromWs> tracker, ServerLineHashesLoader lastLineHashes,
    ActiveRules activeRules, ServerIssueRepository serverIssueRepository, ProjectRepositories projectRepositories, DefaultAnalysisMode mode) {
    this.tracker = tracker;
    this.lastLineHashes = lastLineHashes;
    this.serverIssueRepository = serverIssueRepository;
    this.mode = mode;
    this.activeRules = activeRules;
    this.hasServerAnalysis = projectRepositories.lastAnalysisDate() != null;
  }

  public void init() {
    if (hasServerAnalysis) {
      serverIssueRepository.load();
    }
  }

  public List<TrackedIssue> trackIssues(BatchComponent component, Collection<ScannerReport.Issue> reportIssues, Date analysisDate) {
    List<TrackedIssue> trackedIssues = new LinkedList<>();
    if (hasServerAnalysis) {
      // all the issues that are not closed in db before starting this module scan, including manual issues
      Collection<ServerIssueFromWs> serverIssues = loadServerIssues(component);

      if (shouldCopyServerIssues(component)) {
        // raw issues should be empty, we just need to deal with server issues (SONAR-6931)
        copyServerIssues(serverIssues, trackedIssues);
      } else {

        SourceHashHolder sourceHashHolder = loadSourceHashes(component);
        Collection<TrackedIssue> rIssues = IssueTransformer.toTrackedIssue(component, reportIssues, sourceHashHolder);

        Input<ServerIssueFromWs> baseIssues = createBaseInput(serverIssues, sourceHashHolder);
        Input<TrackedIssue> rawIssues = createRawInput(rIssues, sourceHashHolder);

        Tracking<TrackedIssue, ServerIssueFromWs> track = tracker.track(rawIssues, baseIssues);

        addUnmatchedFromServer(track.getUnmatchedBases(), trackedIssues);
        mergeMatched(track, trackedIssues, rIssues);
        addUnmatchedFromReport(track.getUnmatchedRaws(), trackedIssues, analysisDate);
      }
    }

    if (hasServerAnalysis && ResourceUtils.isRootProject(component.resource())) {
      // issues that relate to deleted components
      addIssuesOnDeletedComponents(trackedIssues);
    }

    return trackedIssues;
  }

  private static Input<ServerIssueFromWs> createBaseInput(Collection<ServerIssueFromWs> serverIssues, @Nullable SourceHashHolder sourceHashHolder) {
    List<String> refHashes;

    if (sourceHashHolder != null && sourceHashHolder.getHashedReference() != null) {
      refHashes = Arrays.asList(sourceHashHolder.getHashedReference().hashes());
    } else {
      refHashes = new ArrayList<>(0);
    }

    return new IssueTrackingInput<>(serverIssues, refHashes);
  }

  private static Input<TrackedIssue> createRawInput(Collection<TrackedIssue> rIssues, @Nullable SourceHashHolder sourceHashHolder) {
    List<String> baseHashes;
    if (sourceHashHolder != null && sourceHashHolder.getHashedSource() != null) {
      baseHashes = Arrays.asList(sourceHashHolder.getHashedSource().hashes());
    } else {
      baseHashes = new ArrayList<>(0);
    }

    return new IssueTrackingInput<>(rIssues, baseHashes);
  }

  private boolean shouldCopyServerIssues(BatchComponent component) {
    if (!mode.scanAllFiles() && component.isFile()) {
      DefaultInputFile inputFile = (DefaultInputFile) component.inputComponent();
      if (inputFile.status() == Status.SAME) {
        return true;
      }
    }
    return false;
  }

  private void copyServerIssues(Collection<ServerIssueFromWs> serverIssues, List<TrackedIssue> trackedIssues) {
    for (ServerIssueFromWs serverIssue : serverIssues) {
      org.sonar.scanner.protocol.input.ScannerInput.ServerIssue unmatchedPreviousIssue = serverIssue.getDto();
      TrackedIssue unmatched = IssueTransformer.toTrackedIssue(unmatchedPreviousIssue);

      ActiveRule activeRule = activeRules.find(unmatched.getRuleKey());
      unmatched.setNew(false);

      if (activeRule == null) {
        // rule removed
        IssueTransformer.resolveRemove(unmatched);
      }

      trackedIssues.add(unmatched);
    }
  }

  @CheckForNull
  private SourceHashHolder loadSourceHashes(BatchComponent component) {
    SourceHashHolder sourceHashHolder = null;
    if (component.isFile()) {
      DefaultInputFile file = (DefaultInputFile) component.inputComponent();
      if (file == null) {
        throw new IllegalStateException("Resource " + component.resource() + " was not found in InputPath cache");
      }
      sourceHashHolder = new SourceHashHolder(file, lastLineHashes);
    }
    return sourceHashHolder;
  }

  private Collection<ServerIssueFromWs> loadServerIssues(BatchComponent component) {
    Collection<ServerIssueFromWs> serverIssues = new ArrayList<>();
    for (org.sonar.scanner.protocol.input.ScannerInput.ServerIssue previousIssue : serverIssueRepository.byComponent(component)) {
      serverIssues.add(new ServerIssueFromWs(previousIssue));
    }
    return serverIssues;
  }

  @VisibleForTesting
  protected void mergeMatched(Tracking<TrackedIssue, ServerIssueFromWs> result, Collection<TrackedIssue> mergeTo, Collection<TrackedIssue> rawIssues) {
    for (Map.Entry<TrackedIssue, ServerIssueFromWs> e : result.getMatchedRaws().entrySet()) {
      org.sonar.scanner.protocol.input.ScannerInput.ServerIssue dto = e.getValue().getDto();
      TrackedIssue tracked = e.getKey();

      // invariant fields
      tracked.setKey(dto.getKey());

      // non-persisted fields
      tracked.setNew(false);

      // fields to update with old values
      tracked.setResolution(dto.hasResolution() ? dto.getResolution() : null);
      tracked.setStatus(dto.getStatus());
      tracked.setAssignee(dto.hasAssigneeLogin() ? dto.getAssigneeLogin() : null);
      tracked.setCreationDate(new Date(dto.getCreationDate()));

      if (dto.getManualSeverity()) {
        // Severity overriden by user
        tracked.setSeverity(dto.getSeverity().name());
      }
      mergeTo.add(tracked);
    }
  }

  private void addUnmatchedFromServer(Iterable<ServerIssueFromWs> unmatchedIssues, Collection<TrackedIssue> mergeTo) {
    for (ServerIssueFromWs unmatchedIssue : unmatchedIssues) {
      org.sonar.scanner.protocol.input.ScannerInput.ServerIssue unmatchedPreviousIssue = unmatchedIssue.getDto();
      TrackedIssue unmatched = IssueTransformer.toTrackedIssue(unmatchedPreviousIssue);
      updateUnmatchedIssue(unmatched);
      mergeTo.add(unmatched);
    }
  }

  private static void addUnmatchedFromReport(Iterable<TrackedIssue> rawIssues, Collection<TrackedIssue> trackedIssues, Date analysisDate) {
    for (TrackedIssue rawIssue : rawIssues) {
      rawIssue.setCreationDate(analysisDate);
      trackedIssues.add(rawIssue);
    }
  }

  private void addIssuesOnDeletedComponents(Collection<TrackedIssue> issues) {
    for (org.sonar.scanner.protocol.input.ScannerInput.ServerIssue previous : serverIssueRepository.issuesOnMissingComponents()) {
      TrackedIssue dead = IssueTransformer.toTrackedIssue(previous);
      updateUnmatchedIssue(dead);
      issues.add(dead);
    }
  }

  private void updateUnmatchedIssue(TrackedIssue issue) {
    ActiveRule activeRule = activeRules.find(issue.getRuleKey());
    issue.setNew(false);

    boolean isRemovedRule = activeRule == null;

    if (isRemovedRule) {
      IssueTransformer.resolveRemove(issue);
    } else {
      IssueTransformer.close(issue);
    }
  }
}
