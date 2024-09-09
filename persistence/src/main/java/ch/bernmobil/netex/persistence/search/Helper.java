package ch.bernmobil.netex.persistence.search;

import java.util.ArrayList;
import java.util.List;

public final class Helper {

  private Helper() {}

  public static <T> List<T> iterableToList(final Iterable<T> iterable) {
    final List<T> result = new ArrayList<>();
    iterable.forEach(result::add);
    return result;
  }
}
