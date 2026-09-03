package io.depsight.api.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ChunkerTest {

  @Test
  void shouldReturnEmptyListWhenInputIsEmpty() {
    List<List<String>> result = Chunker.chunk(List.of(), 1000);
    assertTrue(result.isEmpty());
  }

  @Test
  void shouldReturnOneChunkWhenItemsAreBelowMaximum() {
    List<String> items = List.of("A", "B", "C");

    List<List<String>> result = Chunker.chunk(items, 1000);
    assertEquals(1, result.size());
    assertEquals(items, result.get(0));
  }

  @Test
  void shouldReturnOneChunkWhenItemsEqualMaximum() {
    List<Integer> items = IntStream.range(0, 1000).boxed().toList();

    List<List<Integer>> result = Chunker.chunk(items, 1000);

    assertEquals(1, result.size());
    assertEquals(1000, result.get(0).size());
    assertEquals(items, result.get(0));
  }

  @Test
  void shouldSplitItemsIntoChunksOfMaximumSize() {
    List<Integer> items = IntStream.range(0, 2500).boxed().toList();

    List<List<Integer>> result = Chunker.chunk(items, 1000);
    assertEquals(3, result.size());
    assertEquals(1000, result.get(0).size());
    assertEquals(1000, result.get(1).size());
    assertEquals(500, result.get(2).size());
  }

  @Test
  void shouldPreserveOrderAndContainAllItems() {
    List<Integer> items = IntStream.range(0, 2500).boxed().toList();

    List<List<Integer>> result = Chunker.chunk(items, 1000);

    List<Integer> flattened = result.stream().flatMap(List::stream).toList();

    assertEquals(items, flattened);
  }

  @Test
  void shouldRejectZeroChunkSize() {
    assertThrows(IllegalArgumentException.class, () -> Chunker.chunk(List.of("A", "B"), 0));
  }

  @Test
  void shouldRejectNegativeChunkSize() {
    assertThrows(IllegalArgumentException.class, () -> Chunker.chunk(List.of("A", "B"), -1));
  }

  @Test
  void shouldNotModifyOriginalList() {
    List<String> items = new ArrayList<>(List.of("A", "B", "C", "D"));
    List<String> original = new ArrayList<>(items);

    Chunker.chunk(items, 2);
    assertEquals(original, items);
  }
}
