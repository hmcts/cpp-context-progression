package uk.gov.moj.cpp.data.anonymization;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilderFrom;

import org.junit.Before;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EventStoreDataAnonymizerTest {
    private static final String CASE_ID = "9b42e998-158a-4683-8073-8e9453fe6cc9";

    private EventStoreDataAnonymizer eventStoreDataAnonymizer;

    @Before
    public void initialize() throws IOException {
        eventStoreDataAnonymizer = new EventStoreDataAnonymizer();
    }

    @Test
    public void shouldCreateSendingSheetCompletedEventTransformation() throws IOException {
        eventStoreDataAnonymizer.setEnveloper(EnveloperFactory.createEnveloper());
        final JsonEnvelope event = buildSendingSheetCompletedEnvelope();
        final Stream<JsonEnvelope> jsonEnvelopeStream = eventStoreDataAnonymizer.apply(event);
        Optional<JsonEnvelope> optionalJsonEnvelope = jsonEnvelopeStream.findFirst();

        if (optionalJsonEnvelope.isPresent()) {
            final JsonEnvelope jsonEnvelope = optionalJsonEnvelope.get();
            final JsonObject hearingObject = jsonEnvelope.payloadAsJsonObject().getJsonObject("hearing");
            assertThat("caseId ", hearingObject.getString("caseId"), is(CASE_ID));
            final JsonObject defendantsObject = hearingObject.getJsonArray("defendants").getJsonObject(0);
            assertThat(defendantsObject.getString("firstName"), equalTo("XXXXX"));
            final JsonObject addressObject = defendantsObject.getJsonObject("address");
            assertThat(addressObject.getString("postcode"), equalTo("AA1 1AA"));
            final JsonObject interpretorObject = defendantsObject.getJsonObject("interpreter");
            assertThat(interpretorObject.getBoolean("needed"), equalTo(false));
            final JsonObject offencesObject = defendantsObject.getJsonArray("offences").getJsonObject(0);
            assertThat(offencesObject.getString("offenceCode"), equalTo("OF61131"));
            final JsonObject pleaObject = offencesObject.getJsonObject("plea");
            assertThat(pleaObject.getString("value"), equalTo("GUILTY"));
            final JsonObject crwonCourtHearingObject = jsonEnvelope.payloadAsJsonObject().getJsonObject("crownCourtHearing");
            assertThat(crwonCourtHearingObject.getString("courtCentreName"), equalTo("Liverpool Crown Court"));
        }
        else {
            fail("EventStoreDataAnonymizer failed to load.");
        }
    }

    private JsonEnvelope buildSendingSheetCompletedEnvelope() throws IOException {

        final StringWriter stringWriter = new StringWriter();
        final InputStream stream = EventStoreDataAnonymizerTest.class.getResourceAsStream("/test-data.json");
        IOUtils.copy(stream, stringWriter, UTF_8);
        final JsonReader jsonReader = Json.createReader(new StringReader(stringWriter.toString()));
        final JsonObject payload = jsonReader.readObject();
        jsonReader.close();

        MetadataBuilder metadataBuilder = metadataBuilderFrom(createObjectBuilder().add("id", randomUUID().toString()).add("name", "data.anon.test-event2").build());
        metadataBuilder.withStreamId(fromString(CASE_ID));

        return envelopeFrom(metadataBuilder.withStreamId(fromString(CASE_ID)).build(), payload);
    }


}
