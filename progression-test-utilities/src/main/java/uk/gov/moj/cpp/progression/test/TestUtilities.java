package uk.gov.moj.cpp.progression.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

@SuppressWarnings({"squid:S1118"})
public class TestUtilities {

    public static <T> T with(T object, Consumer<T> consumer) {
        consumer.accept(object);
        return object;
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> List<T> asList(T... a) {
        return new ArrayList<>(Arrays.asList(a));
    }

    public static <T> T at(Collection<T> item, int index) {
        final Iterator<T> it = item.iterator();
        T o = null;
        for (int i = 0; i <= index; i++) {
            o = it.next();
        }
        return o;
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> Set<T> asSet(T... a) {
        return new HashSet<>(Arrays.asList(a));
    }

    public static <T> Matcher<T> print() {
        return new BaseMatcher<T>() {
            @Override
            public boolean matches(Object o) {
                return true;
            }

            @Override
            public void describeTo(Description description) {
                //not required
            }
        };

    }
}
