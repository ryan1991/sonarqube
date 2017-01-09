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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.server.es.EsTester;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentIndexTest {

  private static final String BLA = "bla";
  private static final String UUID_DOC = "UUID-DOC-";
  private static final String UUID_DOC_1 = UUID_DOC + "1";
  private static final String KEY = "KEY-";
  private static final String KEY_1 = KEY + "1";

  private static final String PREFIX = "prefix";
  private static final String MIDDLE = "middle";
  private static final String SUFFIX = "suffix";
  private static final String PREFIX_MIDDLE_SUFFIX = PREFIX + MIDDLE + SUFFIX;

  @Rule
  public EsTester es = new EsTester(new ComponentIndexDefinition(new MapSettings()));

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ComponentIndex index;
  private ComponentIndexer indexer;

  @Before
  public void setUp() {
    index = new ComponentIndex(db.getDbClient(), es.client());
    DbClient dbClient = DbTester.create(System2.INSTANCE).getDbClient();
    indexer = new ComponentIndexer(dbClient, es.client());
  }

  @Test
  public void empty_search() {
    assertSearch(emptyList(), BLA, emptyList());
  }

  @Test
  public void exact_match_search() {
    assertMatch(BLA, BLA);
  }

  @Test
  public void ignore_case() {
    assertMatch("bLa", "BlA");
  }

  @Test
  public void prefix_match_search() {
    assertMatch(PREFIX_MIDDLE_SUFFIX, PREFIX);
  }

  @Test
  public void middle_match_search() {
    assertMatch(PREFIX_MIDDLE_SUFFIX, MIDDLE);
  }

  @Test
  public void suffix_match_search() {
    assertMatch(PREFIX_MIDDLE_SUFFIX, SUFFIX);
  }

  @Test
  public void do_not_interpret_input() {
    assertNotMatch(BLA, "*");
  }

  @Test
  public void key_match_search() {
    assertSearch(
      asList(newDoc(UUID_DOC_1, "name is not a match", "matchingKey", Qualifiers.PROJECT)),
      "matchingKey",
      asList(UUID_DOC_1));
  }

  @Test
  public void unmatching_search() {
    assertNotMatch(BLA, "blubb");
  }

  @Test
  public void limit_number_of_documents() {
    Collection<ComponentDoc> docs = IntStream
      .rangeClosed(1, 42)
      .mapToObj(i -> newDoc(UUID_DOC + i, BLA, KEY + i, Qualifiers.PROJECT))
      .collect(Collectors.toList());

    int pageSize = 41;
    assertThat(search(docs, new ComponentIndexQuery(Qualifiers.PROJECT, BLA, pageSize))).hasSize(pageSize);
  }

  private void assertMatch(String name, String query) {
    assertSearch(
      asList(newDoc(name)),
      query,
      asList(UUID_DOC_1));
  }

  private void assertNotMatch(String name, String query) {
    assertSearch(
      asList(newDoc(name)),
      query,
      emptyList());
  }

  private ComponentDoc newDoc(String name) {
    return newDoc(UUID_DOC_1, name);
  }

  private ComponentDoc newDoc(String uuid, String name) {
    return newDoc(uuid, name, KEY_1, Qualifiers.PROJECT);
  }

  private ComponentDoc newDoc(String uuid, String name, String key, String qualifier) {
    ComponentDoc doc = new ComponentDoc();
    doc.setId(uuid);
    doc.setName(name);
    doc.setKey(key);
    doc.setQualifier(qualifier);
    return doc;
  }

  private void assertSearch(Collection<ComponentDoc> input, String queryText, Collection<String> expectedOutput) {
    assertThat(search(input, queryText))
      .hasSameElementsAs(expectedOutput);
  }

  private List<String> search(Collection<ComponentDoc> input, String query) {
    return search(input, new ComponentIndexQuery(Qualifiers.PROJECT, query));
  }

  private List<String> search(Collection<ComponentDoc> input, ComponentIndexQuery query) {
    input.stream().forEach(indexer::index);
    return index.searchIds(query);
  }
}
