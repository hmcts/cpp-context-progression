package uk.gov.justice.api.resource.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.api.resource.utils.ResultPromptValueHelper.getValue;

import javax.json.Json;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;

public class ResultPromptValueHelperTest {

    @Test
    public void shouldConvertBooleanTypePromptValueToStringValue() {
        assertThat(getValue("BOOLEAN", JsonValue.TRUE), is("true"));
    }

    @Test
    public void shouldConvertYesBoxTypePromptValueToStringValue() {
        assertThat(getValue("YESBOX", JsonValue.TRUE), is("true"));
    }

    @Test
    public void shouldConvertCurrencyTypePromptValueToStringValue() {
        assertThat(getValue("CURR", Json.createValue(1200.00)), is("1200.00"));
    }

    @Test
    public void shouldConvertDurationTypePromptValueToStringValue() {
        assertThat(getValue("DURATION", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("label", "MINUTES")
                        .add("value", 30)
                        .build())
                .build()), is("30 MINUTES"));
    }

    @Test
    public void shouldConvertDurationTypePromptWithMultipleValuesToStringValue() {
        assertThat(getValue("DURATION", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("label", "HOURS")
                        .add("value", 2)
                        .build())
                .add(Json.createObjectBuilder()
                        .add("label", "MINUTES")
                        .add("value", 30)
                        .build())
                .build()), is("2 HOURS 30 MINUTES"));
    }

    @Test
    public void shouldConvertFixedListMultipleTypePromptValueToStringValue() {
        assertThat(getValue("FIXLM", Json.createArrayBuilder()
                .add("Offence while on bail")
                .build()), is("Offence while on bail"));
    }

    @Test
    public void shouldConvertFixedListMultipleTypePromptMultipleValuesToStringValue() {
        assertThat(getValue("FIXLM", Json.createArrayBuilder()
                .add("Offence while on bail")
                .add("Second element")
                .build()), is("Offence while on bail###Second element"));
    }

    @Test
    public void shouldConvertFixedListOtherMultipleTypePromptValueToStringValue() {
        assertThat(getValue("FIXLOM", Json.createArrayBuilder()
                .add("Broken bail conditions")
                .build()), is("Broken bail conditions"));
    }

    @Test
    public void shouldConvertFixedListOtherMultipleTypePromptMultipleValuesToStringValue() {
        assertThat(getValue("FIXLOM", Json.createArrayBuilder()
                .add("Broken bail conditions")
                .add("Offended on bail")
                .build()), is("Broken bail conditions###Offended on bail"));
    }


}