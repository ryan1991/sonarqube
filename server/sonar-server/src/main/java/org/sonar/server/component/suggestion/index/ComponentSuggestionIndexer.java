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
import java.util.stream.Stream;
import org.elasticsearch.action.index.IndexRequest;
import org.sonar.api.Startable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
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

  /**
   * Copy all database contents to the elastic search index.
   */
  public void index() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.componentDao()
        .selectAll(dbSession, context -> index((ComponentDto) context.getResultObject()));
    }
  }

  /**
   * Update the elastic search for one specific project. The current data from the database is used.
   */
  public void index(String projectUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      index(dbClient.componentDao().selectByProjectUuid(projectUuid, dbSession).stream());
    }
  }

  private void index(Stream<ComponentDto> components) {
    components.forEach(this::index);
  }

  public void index(ComponentDto doc) {
    index(toDocument(doc));
  }

  public void index(ComponentSuggestionDoc document) {
    Future<?> submit = executor.submit(() -> indexNow(document));
    try {
      Uninterruptibles.getUninterruptibly(submit);
    } catch (ExecutionException e) {
      Throwables.propagate(e);
    }
  }

  private void indexNow(ComponentSuggestionDoc doc) {
    BulkIndexer bulk = new BulkIndexer(esClient, INDEX_COMPONENT_SUGGESTION);// TODO reuse bulk indexer
    bulk.setLarge(false);
    bulk.start();
    bulk.add(newIndexRequest(doc));
    bulk.stop();
  }

  private static IndexRequest newIndexRequest(ComponentSuggestionDoc doc) {
    return new IndexRequest(INDEX_COMPONENT_SUGGESTION, TYPE_COMPONENT_SUGGESTION, doc.getId())
      .source(doc.getFields());
  }

  private static ComponentSuggestionDoc toDocument(ComponentDto component) {
    return new ComponentSuggestionDoc()
      .setId(component.uuid())
      .setName(component.name())
      .setKey(component.key())
      .setQualifier(component.qualifier());
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
