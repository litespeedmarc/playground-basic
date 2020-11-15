package com.scibrazeau.interview;

import com.scibrazeau.interview.utils.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Array;
import java.text.CollationElementIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class CollectionUtilsTest {
    @Test
    public void testGetFirstNullInput() {
        Assert.assertNull(CollectionUtils.first(null));
        Assert.assertNull(CollectionUtils.first(createList()));
        Assert.assertEquals((Integer)5, CollectionUtils.first(createList(5, -1)));
    }

    private Collection<Integer> createList(int ... ints) {
        ArrayList<Integer> list = new ArrayList<>();
        Arrays.stream(ints).forEach(list::add);
        return list;
    }
}
