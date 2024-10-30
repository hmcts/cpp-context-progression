package uk.gov.moj.cpp.progression.test.matchers;

import java.util.Collection;
import java.util.Iterator;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ElementAtListMatcher extends TypeSafeMatcher<Collection<?>> {

    private int index;
    private Matcher<?> matcher;
    private Error error;

    private ElementAtListMatcher(int index, Matcher<?> matcher) {
        this.index = index;
        this.matcher = matcher;
    }

    @Override
    protected boolean matchesSafely(Collection<?> item) {

        if (item == null) {
            this.error = Error.NULL;
            return false;
        }

        if (this.index < 0 || this.index >= item.size()) {
            this.error = Error.INVALID_INDEX;
            return false;
        }

        if (!this.matcher.matches(get(item, index))) {
            this.error = Error.MATCHER_FALSE;
            return false;
        }

        return true;
    }

    private static Object get(Collection<?> item, int index) {
        final Iterator it = item.iterator();
        Object o = null;
        for (int i = 0; i <= index; i++) {
            o = it.next();
        }
        return o;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("[").appendText(Integer.toString(this.index)).appendText("]");

        if (this.error == Error.NULL) {
            description.appendText(" is a list object");
        }

        if (this.error == Error.MATCHER_FALSE) {
            description.appendDescriptionOf(this.matcher);
        }
        if (this.error == Error.INVALID_INDEX) {
            description.appendText(" is present");
        }
    }

    @Override
    protected void describeMismatchSafely(Collection<?> item, Description mismatchDescription) {

        if (this.error == Error.NULL) {
            mismatchDescription.appendText("was null");
        }

        if (this.error == Error.MATCHER_FALSE) {
            this.matcher.describeMismatch(get(item, index), mismatchDescription);
        }

        if (this.error == Error.INVALID_INDEX) {
            mismatchDescription.appendText("list has no element for index ").appendText(Integer.toString(this.index));
        }
    }

    private enum Error {
        NULL, INVALID_INDEX, MATCHER_FALSE
    }

    public static ElementAtListMatcher first(Matcher<?> matcher) {
        return new ElementAtListMatcher(0, matcher);
    }

    public static ElementAtListMatcher second(Matcher<?> matcher) {
        return new ElementAtListMatcher(1, matcher);
    }

    public static ElementAtListMatcher third(Matcher<?> matcher) {
        return new ElementAtListMatcher(2, matcher);
    }

    public static ElementAtListMatcher fourth(Matcher<?> matcher) {
        return new ElementAtListMatcher(3, matcher);
    }

    public static ElementAtListMatcher fifth(Matcher<?> matcher) {
        return new ElementAtListMatcher(4, matcher);
    }

    public static ElementAtListMatcher elementAt(int index, Matcher<?> matcher) {
        return new ElementAtListMatcher(index, matcher);
    }
}
