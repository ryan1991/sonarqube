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
import org.sonar.api.utils.Paging;
import org.sonar.api.utils.System2;
import org.sonar.db.component.ComponentQuery;
import org.sonar.server.es.EsTester;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentSuggestionIndexTest {

  private static final int PAGE_SIZE = 6;
  private static final String[] QUALIFIERS = {Qualifiers.VIEW, Qualifiers.SUBVIEW, Qualifiers.PROJECT, Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE,
    Qualifiers.DIRECTORY, Qualifiers.MODULE};

  @Rule
  public EsTester es = new EsTester(new ComponentSuggestionIndexDefinition(new MapSettings()));

  private ComponentSuggestionIndex index;
  private ComponentSuggestionIndexer indexer;

  @Before
  public void setUp() {
    index = new ComponentSuggestionIndex(es.client());
    indexer = new ComponentSuggestionIndexer(System2.INSTANCE, es.client());
  }

  @Test
  public void empty_search() {
    assertSearch(
      Arrays.asList(),
      "bla",
      Collections.emptyList());
  }

  @Test
  public void exact_match_search() {
    assertSearch(
      Arrays.asList(
        newDoc("UUID-DOC-1", "bla")),
      "bla",
      Arrays.asList(
        "UUID-DOC-1"));
  }

  @Test
  public void unmatching_search() {
    assertSearch(
      Arrays.asList(
        newDoc("UUID-DOC-1", "bla")),
      "blubb",
      Collections.emptyList());
  }

  @Test
  public void limit_number_of_documents() {
    Collection<ComponentSuggestionDoc> docs = IntStream.rangeClosed(1, PAGE_SIZE + 1).mapToObj(i -> newDoc("UUID-DOC-" + i, "bla")).collect(Collectors.toList());
    assertThat(search(docs, "bla")).hasSize(PAGE_SIZE);
  }

  @Test
  public void limit_number_of_documents_per_qualifier() {
    Collection<ComponentSuggestionDoc> docs = Arrays.stream(QUALIFIERS)
      .map(q -> newDoc("UUID-DOC-" + q, "bla", q)).collect(Collectors.toList());
    Collection<String> results = search(
      docs,
      "bla");
    assertThat(results).hasSameElementsAs(docs.stream().map(d -> d.getId()).collect(Collectors.toList()));
  }

  private ComponentSuggestionDoc newDoc(String uuid, String name) {
    return newDoc(uuid, name, Qualifiers.PROJECT);
  }

  private ComponentSuggestionDoc newDoc(String uuid, String name, String qualifier) {
    ComponentSuggestionDoc doc = new ComponentSuggestionDoc();
    doc.setId(uuid);
    doc.setQualifier(qualifier);
    doc.setName(name);
    return doc;
  }

  private void assertSearch(Collection<ComponentSuggestionDoc> input, String queryText, Collection<String> expectedOutput) {
    List<String> result = search(input, queryText);
    assertThat(result).hasSameElementsAs(expectedOutput);
  }

  private List<String> search(Collection<ComponentSuggestionDoc> input, String queryText) {
    input.stream().forEach(indexer::index);
    ComponentQuery query = ComponentQuery.builder().setNameOrKeyQuery(queryText).setQualifiers(QUALIFIERS).build();
    return index.search(query, Paging.forPageIndex(1).withPageSize(PAGE_SIZE).andTotal(PAGE_SIZE));
  }
}
