package org.sonar.server.component.suggestion.index;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.EsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.INDEX_COMPONENT_SUGGESTION;
import static org.sonar.server.component.suggestion.index.ComponentSuggestionIndexDefinition.TYPE_COMPONENT_SUGGESTION;

public class ComponentSuggestionIndexerTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public EsTester esTester = new EsTester(new ComponentSuggestionIndexDefinition(new MapSettings()));

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();

  @Test
  public void index_nothing() {
    assertThat(indexAndReturnCount()).isZero();
  }

  @Test
  public void index() {
    ComponentDto component = new ComponentDto()
      .setKey("KEY-1")
      .setUuid("UUID-1")
      .setRootUuid("ROOT-1")
      .setUuidPath("PATH-1")
      .setQualifier(Qualifiers.PROJECT)
      .setName("Name")
      .setDescription("Description");
    dbClient.componentDao().insert(dbSession, component);
    dbSession.commit();

    assertThat(indexAndReturnCount()).isEqualTo(1);
  }

  private long indexAndReturnCount() {
    ComponentSuggestionIndexer indexer = createIndexer();
    indexer.index();
    long countDocuments = esTester.countDocuments(INDEX_COMPONENT_SUGGESTION, TYPE_COMPONENT_SUGGESTION);
    return countDocuments;
  }

  private ComponentSuggestionIndexer createIndexer() {
    return new ComponentSuggestionIndexer(dbTester.getDbClient(), esTester.client());
  }

}
