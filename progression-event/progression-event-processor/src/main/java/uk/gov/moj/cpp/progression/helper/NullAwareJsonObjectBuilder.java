package uk.gov.moj.cpp.progression.helper;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/**
 *
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class NullAwareJsonObjectBuilder implements JsonObjectBuilder {

    private final JsonObjectBuilder builder;

    public static JsonObjectBuilder wrap(final JsonObjectBuilder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Builder must not be null");
        }
        return new NullAwareJsonObjectBuilder(builder);
    }

    private NullAwareJsonObjectBuilder(final JsonObjectBuilder builder) {
        this.builder = builder;
    }
    @Override
    public JsonObjectBuilder add(final String name, final JsonValue value) {
        if (name != null && value != null) {
            builder.add(name, value);
        }
        return this;

    }

    @Override
    public JsonObjectBuilder add(final String name, final String value) {
        if (name != null && value != null) {
            builder.add(name, value);
        }
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final BigInteger value) {
        if (name != null && value != null) {
            builder.add(name, value);
        }
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final BigDecimal value) {
        if (name != null && value != null) {
            builder.add(name, value);
        }
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final int value) {
        if (name != null) {
            builder.add(name, value);
        }
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final long value) {
        if (name != null) {
            builder.add(name, value);
        }
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final double value) {
        if (name != null) {
            builder.add(name, value);
        }
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final boolean value) {
        if (name != null) {
            builder.add(name, value);
        }
        return this;
    }

    @Override
    public JsonObjectBuilder addNull(final String name) {
        if (name != null) {
            builder.add(name, JsonValue.NULL);
        }
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final JsonObjectBuilder objectBuilder) {
        if (name != null && objectBuilder != null) {
            builder.add(name, objectBuilder);
        }
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final JsonArrayBuilder arrayBuilder) {
        if (name != null && arrayBuilder != null) {
            builder.add(name, arrayBuilder);
        }
        return this;
    }

    @Override
    public JsonObject build() {
        return builder.build();
    }

}
