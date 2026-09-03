package io.depsight.api.common.util;

import java.util.ArrayList;
import java.util.List;

public class Chunker {

  public static <T> List<List<T>> chunk(List<T> items, int maxChunkSize) {
    if (maxChunkSize <= 0) {
      throw new IllegalArgumentException("maxChunkSize should be greater than 0");
    }

    if (items.isEmpty()) {
      return List.of();
    }

    List<List<T>> result = new ArrayList<>();

    for (int start = 0; start < items.size(); start += maxChunkSize) {
      int end = Math.min(start + maxChunkSize, items.size());
      result.add(new ArrayList<>(items.subList(start, end)));
    }

    return result;
  }
}
