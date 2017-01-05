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

package org.sonar.server.component.suggestion.index;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.api.Startable;
import org.sonar.db.DbClient;
import org.sonar.server.es.BulkIndexer;
import org.sonar.server.es.EsClient;

import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.INDEX_COMPONENT_SUGGESTION;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.TYPE_COMPONENT_SUGGESTION;

public class ComponentSuggestionIndexer implements Startable {

  private final ThreadPoolExecutor executor;// TODO avoid duplications from PermissionIndexer
  private final DbClient dbClient;
  private final EsClient esClient;

  public ComponentSuggestionIndexer(DbClient dbClient, EsClient esClient) {
    this.executor = new ThreadPoolExecutor(0, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    this.dbClient = dbClient;
    this.esClient = esClient;
  }

  public void index(ComponentSuggestionDoc doc) {
    Future<?> submit = executor.submit(() -> {
      // try (DbSession dbSession = dbClient.openSession(false)) {
      // index(new ComponentSuggestionIndexer().selectAll(dbClient, dbSession));
      // }
      indexNow(doc);
    });
    try {
      Uninterruptibles.getUninterruptibly(submit);
    } catch (ExecutionException e) {
      Throwables.propagate(e);
    }
  }

  private void indexNow(ComponentSuggestionDoc doc) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_COMPONENT_SUGGESTION);
    bulk.setLarge(false);
    bulk.start();
    bulk.add(newIndexRequest(doc));
    bulk.stop();
  }

  private static IndexRequest newIndexRequest(ComponentSuggestionDoc doc) {
    return new IndexRequest(INDEX_COMPONENT_SUGGESTION, TYPE_COMPONENT_SUGGESTION, doc.getId())
      .source(doc.getFields());
  }

  @Override
  public void start() {
    // no action required, setup is done in constructor
  }

  @Override
  public void stop() {
    executor.shutdown();
  }
}
