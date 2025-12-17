package uk.gov.moj.cpp.progression.test.matchers;

import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.SelfDescribing;

@SuppressWarnings({"squid:S1166", "squid:S1141", "squid:S00108", "squid:S2221", "squid:S00112", "squid:S2129"})
public class BeanMatcher<T> extends BaseMatcher<T> {

    private Class<T> clazz;
    private Error error;
    private String methodName;
    private Assertion<T, ?> failedAssertion;
    private List<Assertion<T, ?>> assertions = new ArrayList<>();

    private BeanMatcher(Class<T> clazz) {
        this.clazz = clazz;
    }

    public <R> BeanMatcher<T> with(Function<T, R> accessor, Matcher<R> matcher) {
        assertions.add(new Assertion<>(accessor, matcher));
        return this;
    }

    public <R> BeanMatcher<T> withOptional(Function<T, Optional<R>> accessor, Matcher<R> underlyingMatcher) {
        final Matcher<Optional<R>> matcher = new BaseMatcher<Optional<R>>() {
            @Override
            public boolean matches(Object o) {
                return underlyingMatcher.matches(o == null ? null : ((Optional) o).orElse(null));
            }

            @Override
            public void describeTo(Description description) {
                underlyingMatcher.describeTo(description);
            }
        };
        assertions.add(new Assertion<>(accessor, matcher));
        return this;
    }

    public <R> BeanMatcher<T> withValue(final Function<T, R> accessor, final R value) {
        assertions.add(new Assertion<>(accessor, is(value)));
        return this;
    }

    public <R> BeanMatcher<T> withOptionalValue(final Function<T, Optional<R>> accessor, final R value) {
        final Matcher<R> underlyingMatcher = is(value);
        final Matcher<Optional<R>> matcher = new BaseMatcher<Optional<R>>() {
            @Override
            public boolean matches(Object o) {
                return underlyingMatcher.matches(o == null ? null : ((Optional) o).orElse(null));
            }

            @Override
            public void describeTo(Description description) {
                underlyingMatcher.describeTo(description);
            }
        };
        assertions.add(new Assertion<>(accessor, matcher));
        return this;
    }

    @Override
    public boolean matches(Object item) {

        if (item == null) {
            this.error = Error.NULL;
            return false;
        }

        if (!clazz.isInstance(item)) {
            this.error = Error.INVALID_TYPE;
            return false;
        }

        Class<?> loadedProxyClazz;
        try (DynamicType.Unloaded<T> unloaded = new ByteBuddy().subclass(clazz).method(ElementMatchers.any())
                .intercept(InvocationHandlerAdapter.of((proxyArg, method, args) -> {
                    methodName = method.getName();
                    return method.invoke(item, args);
                })).make()) {
            loadedProxyClazz = unloaded.load(clazz.getClassLoader()).getLoaded();
        } catch (IOException e) {
            throw new RuntimeException("Unable to create proxy for class: " + clazz.getName(), e);
        }

        try {
            final T proxy = (T) create(loadedProxyClazz, clazz);

            for (final Assertion<T, ?> assertion : assertions) {
                if (!assertion.getMatcher().matches(assertion.getAccessor().apply(proxy))) {
                    this.failedAssertion = assertion;
                    this.error = Error.INVALID_ASSERTION;
                    return false;
                }
            }

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid super clazz: " + clazz.getName(), e);
        }
        return true;
    }

    private Object create(final Class<?> proxyClazz, final Class<?> theClazz) {
        try {
            //try the default constructor first
            final Class<?>[] noArgs = {};
            Constructor<?> constructor = null;
            try {
                constructor = theClazz.getDeclaredConstructor(noArgs);
            } catch (Exception ex) {
            }
            if (constructor != null && Modifier.isPublic(constructor.getModifiers())) {
                return proxyClazz.getDeclaredConstructor().newInstance();
            }
            // try to find a non default constructor
            final Optional<Constructor<?>> nonDefaultConstructor = Arrays.asList(theClazz.getConstructors()).stream()
                    .filter(c -> c.getParameterTypes().length > 0).findFirst();

            final Class<?>[] paramTypes = nonDefaultConstructor.orElseThrow(
                    () -> new RuntimeException("failed to find non default constructor for " + theClazz.getCanonicalName())).getParameterTypes();

            final Object[] parameters = new Object[paramTypes.length];
            for (int done = 0; done < parameters.length; done++) {
                parameters[done] = getExampleParamValue(paramTypes[done]);
            }
            return proxyClazz.getDeclaredConstructor(paramTypes).newInstance(parameters);
        } catch (Exception ex) {
            throw new RuntimeException("failed to create a " + theClazz.getCanonicalName(), ex);
        }
    }

    private Object getExampleParamValue(Class theClass) {
        if (Integer.TYPE == theClass) {
            return Integer.valueOf(1);
        } else if (Long.TYPE == theClass) {
            return Long.valueOf(1);
        } else if (Boolean.TYPE == theClass) {
            return true;
        } else if (Float.TYPE == theClass) {
            return Float.valueOf(1);
        } else if (Double.TYPE == theClass) {
            return Double.valueOf(1);
        } else if (Short.TYPE == theClass) {
            return Short.valueOf((short) 1);
        } else if (Byte.TYPE == theClass) {
            return Byte.valueOf(((byte) 1));
        } else if (Character.TYPE == theClass) {
            return Character.valueOf(('a'));
        } else {
            return null;
        }
    }

    @Override
    public void describeTo(Description description) {
        Description descriptionWrapper;
        if (Proxy.isProxyClass(description.getClass())) {
            descriptionWrapper = description;
        } else {
            descriptionWrapper = (Description) Proxy.newProxyInstance(Description.class.getClassLoader(),
                    new Class[]{Description.class}, new DescriptionProxyHandler(description));
        }

        if (error == Error.NULL) {
            descriptionWrapper.appendText(" to not be null");
        }

        if (error == Error.INVALID_TYPE) {
            descriptionWrapper.appendText(" to be of type ").appendText(this.clazz.getCanonicalName());
        }

        if (error == Error.INVALID_ASSERTION) {
            descriptionWrapper.appendText(".").appendText(this.methodName).appendText("()").appendDescriptionOf(this.failedAssertion.getMatcher());
        }
    }


    @Override
    public void describeMismatch(Object item, Description description) {

        if (error == Error.NULL) {
            description.appendText("was null");
        }

        if (error == Error.INVALID_TYPE) {
            description.appendText("was of type ").appendText(item.getClass().getCanonicalName());
        }

        if (error == Error.INVALID_ASSERTION) {
            this.failedAssertion.getMatcher().describeMismatch(this.failedAssertion.getAccessor().apply((T) item), description);
        }
    }

    private enum Error {
        NULL,
        INVALID_ASSERTION,
        INVALID_TYPE
    }

    private static class DescriptionProxyHandler implements InvocationHandler {

        private boolean latch = false;
        private Description description;

        DescriptionProxyHandler(Description description) {
            this.description = description;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!latch) {
                this.description.appendText("this");
            }
            latch = true;

            if ("appendDescriptionOf".equals(method.getName())) {
                if (!(args[0] instanceof BeanMatcher || args[0] instanceof ElementAtListMatcher || args[0] instanceof CollectionSearchMatcher)) {
                    description.appendText(" ");
                }
                final SelfDescribing selfDescribing = (SelfDescribing) args[0];
                selfDescribing.describeTo((Description) proxy);
                return proxy;
            }
            method.invoke(description, args);
            return proxy;
        }
    }

    private static class Assertion<T, R> {

        private Function<T, R> accessor;
        private Matcher<R> matcher;

        private Assertion(Function<T, R> accessor, Matcher<R> matcher) {
            this.accessor = accessor;
            this.matcher = matcher;
        }

        Function<T, R> getAccessor() {
            return accessor;
        }

        Matcher<R> getMatcher() {
            return matcher;
        }
    }

    public static <T> BeanMatcher<T> isBean(Class<T> clazz) {
        return new BeanMatcher<>(clazz);
    }
}
