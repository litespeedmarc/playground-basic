package com.scibrazeau.interview.utils;

import java.util.Collection;
import java.util.List;

public class CollectionUtils {
    public static <T> T first(Collection<T> list) {
        return list == null || list.isEmpty() ? null : list instanceof List ? ((List<T>) list).get(0) : list.iterator().next();
    }

}
