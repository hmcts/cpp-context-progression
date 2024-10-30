package uk.gov.moj.cpp.progression.test.matchers;

import java.util.Optional;
import java.util.Set;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class CollectionSearchMatcher extends TypeSafeMatcher<Set<?>> {

    private Matcher<?> predicate;
    private Matcher<?> matcher;
    private Error error;

    private CollectionSearchMatcher(Matcher<?> predicate, Matcher<?> matcher) {
        this.predicate = predicate;
        this.matcher = matcher;
    }

    @Override
    protected boolean matchesSafely(Set<?> item) {

        if (item == null) {
            this.error = Error.NULL;
            return false;
        }

        final Optional<?> opt = item.stream().filter(o -> predicate.matches(o)).findAny();

        if (!opt.isPresent()) {
            this.error = Error.INVALID_FIND;
            return false;
        }

        if (!this.matcher.matches(opt.get())) {
            this.error = Error.MATCHER_FALSE;
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("{");
        predicate.describeTo(description);
        description.appendText(" }");

        if (this.error == Error.NULL) {
            description.appendText(" is a list object");
        }

        if (this.error == Error.MATCHER_FALSE) {
            description.appendDescriptionOf(this.matcher);
        }
        if (this.error == Error.INVALID_FIND) {
            description.appendText(" is present");
        }
    }

    @SuppressWarnings("squid:S3655")
    @Override
    protected void describeMismatchSafely(Set<?> item, Description mismatchDescription) {

        if (this.error == Error.NULL) {
            mismatchDescription.appendText("was null");
        }

        if (this.error == Error.MATCHER_FALSE) {
            final Optional<?> opt = item.stream().filter(o -> predicate.matches(o)).findAny();
            this.matcher.describeMismatch(opt.get(), mismatchDescription);
        }

        if (this.error == Error.INVALID_FIND) {
            mismatchDescription.appendText("set has no element for predicate ");
        }
    }

    private enum Error {
        NULL, INVALID_FIND, MATCHER_FALSE
    }


    public static CollectionSearchMatcher findElement(Matcher<?> predicate, Matcher<?> matcher) {
        return new CollectionSearchMatcher(predicate, matcher);
    }
}
