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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentSuggestionIndexTest {

  private static final int PAGE_SIZE = 6;
  private static final String[] QUALIFIERS = {Qualifiers.VIEW, Qualifiers.SUBVIEW, Qualifiers.PROJECT, Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE,
    Qualifiers.DIRECTORY, Qualifiers.MODULE};

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
  public EsTester es = new EsTester(new ComponentSuggestionIndexDefinition(new MapSettings()));

  private ComponentSuggestionIndex index;
  private ComponentSuggestionIndexer indexer;

  @Before
  public void setUp() {
    index = new ComponentSuggestionIndex(es.client());
    DbClient dbClient = DbTester.create(System2.INSTANCE).getDbClient();
    indexer = new ComponentSuggestionIndexer(dbClient, es.client());
  }

  @Test
  public void empty_search() {
    assertSearch(
      asList(),
      BLA,
      Collections.emptyList());
  }

  @Test
  public void exact_match_search() {
    assertSearch(
      asList(newDoc(BLA)),
      BLA,
      asList(UUID_DOC_1));
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

  private void assertMatch(String name, String query) {
    assertSearch(
      asList(newDoc(name)),
      query,
      asList(UUID_DOC_1));
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
    assertSearch(
      asList(newDoc(BLA)),
      "blubb",
      Collections.emptyList());
  }

  @Test
  public void limit_number_of_documents() {
    Collection<ComponentSuggestionDoc> docs = IntStream
      .rangeClosed(1, PAGE_SIZE + 1)
      .mapToObj(i -> newDoc(UUID_DOC + i, BLA, KEY + i, Qualifiers.PROJECT))
      .collect(Collectors.toList());
    assertThat(search(docs, BLA)).hasSize(PAGE_SIZE);
  }

  @Test
  public void limit_number_of_documents_per_qualifier() {

    // create one document for each qualifier
    Collection<ComponentSuggestionDoc> docs = Arrays
      .stream(QUALIFIERS)
      .map(q -> newDoc(UUID_DOC + q, BLA, KEY + q, q))
      .collect(Collectors.toList());

    List<String> ids = docs.stream().map(d -> d.getId()).collect(Collectors.toList());
    assertThat(search(docs, BLA)).hasSameElementsAs(ids);
  }

  private ComponentSuggestionDoc newDoc(String name) {
    return newDoc(UUID_DOC_1, name);
  }

  private ComponentSuggestionDoc newDoc(String uuid, String name) {
    return newDoc(uuid, name, KEY_1, Qualifiers.PROJECT);
  }

  private ComponentSuggestionDoc newDoc(String uuid, String name, String key, String qualifier) {
    ComponentSuggestionDoc doc = new ComponentSuggestionDoc();
    doc.setId(uuid);
    doc.setName(name);
    doc.setKey(key);
    doc.setQualifier(qualifier);
    return doc;
  }

  private void assertSearch(Collection<ComponentSuggestionDoc> input, String queryText, Collection<String> expectedOutput) {
    assertThat(search(input, queryText))
      .hasSameElementsAs(expectedOutput);
  }

  private List<String> search(Collection<ComponentSuggestionDoc> input, String query) {
    input.stream().forEach(indexer::index);
    return index.search(query);
  }
}
