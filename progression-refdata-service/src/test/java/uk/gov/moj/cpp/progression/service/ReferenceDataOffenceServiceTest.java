package uk.gov.moj.cpp.progression.service;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.moj.cpp.progression.test.FileUtil.givenPayload;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataOffenceServiceTest {

    @InjectMocks
    private ReferenceDataOffenceService referenceDataOffenceService;

    @Mock
    private Requester requester;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeArgumentCaptor;

    @Test
    public void shouldGetMultipleOffencesByOffenceCodeList() throws IOException {
        final String offenceCode1 = randomAlphanumeric(8);
        final String offenceCode2 = randomAlphanumeric(8);

        final List<String> offenceCodes = Arrays.asList(offenceCode1, offenceCode2);

        final JsonEnvelope responseEnvelope = prepareResponseEnvelopeForOffencesList(offenceCode1, offenceCode2,"/referencedataoffences.offences-list.json");

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder()
                .withId(randomUUID())
                .withName("referencedataoffences.query.offences-list"), JsonValue.NULL);

        when(requester.request(any())).thenReturn(responseEnvelope);

        final Optional<List<JsonObject>> offencesJsonObject = referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(offenceCodes, envelope, requester);

        verify(requester).request(envelopeArgumentCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeArgumentCaptor.getValue();
        MatcherAssert.assertThat(capturedEnvelope.metadata().name(), is("referencedataoffences.query.offences-list"));

        assertThat(offencesJsonObject.isPresent(), is(true));
        assertThat(offencesJsonObject.get().size(), is(2));

        final JsonObject offencesJsonObject1 = offencesJsonObject.get().get(0);
        final JsonObject offencesJsonObject2 = offencesJsonObject.get().get(1);

        assertThat(offencesJsonObject1.getString("cjsOffenceCode"), is(offenceCode1));
        assertThat(offencesJsonObject2.getString("cjsOffenceCode"), is(offenceCode2));

        assertThat(offencesJsonObject1.getString("reportRestrictResultCode"), is("YES"));
        assertThat(offencesJsonObject2.getString("reportRestrictResultCode"), is("YES"));

        assertThat(offencesJsonObject1.getString("legislation"), is("Contrary to section 32 of the Sexual Offences Act 2003."));
        assertThat(offencesJsonObject2.getString("legislation"), is("Contrary to section 13 of and Schedule 2 to the Sexual Offences Act 1956."));
    }

    @Test
    public void shouldGetMultipleOffencesByOffenceCodeListWhenLanguageIsWelsh() throws IOException {
        final String offenceCode1 = randomAlphanumeric(8);
        final String offenceCode2 = randomAlphanumeric(8);

        final List<String> offenceCodes = Arrays.asList(offenceCode1, offenceCode2);

        final JsonEnvelope responseEnvelope = prepareResponseEnvelopeForOffencesList(offenceCode1, offenceCode2,"/referencedataoffences.offences-list-welsh.json");

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder()
                .withId(randomUUID())
                .withName("referencedataoffences.query.offences-list"), JsonValue.NULL);

        when(requester.request(any())).thenReturn(responseEnvelope);

        final Optional<List<JsonObject>> offencesJsonObject = referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(offenceCodes, envelope, requester);

        verify(requester).request(envelopeArgumentCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeArgumentCaptor.getValue();
        MatcherAssert.assertThat(capturedEnvelope.metadata().name(), is("referencedataoffences.query.offences-list"));

        assertThat(offencesJsonObject.isPresent(), is(true));
        assertThat(offencesJsonObject.get().size(), is(2));

        final JsonObject offencesJsonObject1 = offencesJsonObject.get().get(0);
        final JsonObject offencesJsonObject2 = offencesJsonObject.get().get(1);

        assertThat(offencesJsonObject1.getString("cjsOffenceCode"), is(offenceCode1));
        assertThat(offencesJsonObject2.getString("cjsOffenceCode"), is(offenceCode2));

        assertThat(offencesJsonObject1.getString("reportRestrictResultCode"), is("YES"));
        assertThat(offencesJsonObject2.getString("reportRestrictResultCode"), is("YES"));

        assertThat(offencesJsonObject1.getString("welshlegislation"), is(""));
        assertThat(offencesJsonObject1.getString("welshoffencetitle"), is("offenceTitle"));
        assertThat(offencesJsonObject2.getString("welshlegislation"), is("welshlegislation"));
    }

    @Test
    public void shouldGetMultipleOffencesByOffenceCodeListWhichOneOfThemWithoutRR() throws IOException {
        // RR : Reporting Restriction
        final String offenceCode1 = randomAlphanumeric(8);
        final String offenceCode2 = randomAlphanumeric(8);

        final List<String> offenceCodes = Arrays.asList(offenceCode1, offenceCode2);

        final JsonEnvelope responseEnvelope = prepareResponseEnvelopeForOffencesListWhichOneOfThemWithoutReportingRestrictions(offenceCode1, offenceCode2);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder()
                .withId(randomUUID())
                .withName("referencedataoffences.query.offences-list"), JsonValue.NULL);

        when(requester.request(any())).thenReturn(responseEnvelope);

        final Optional<List<JsonObject>> offencesJsonObject = referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(offenceCodes, envelope, requester);

        verify(requester).request(envelopeArgumentCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeArgumentCaptor.getValue();
        MatcherAssert.assertThat(capturedEnvelope.metadata().name(), is("referencedataoffences.query.offences-list"));

        assertThat(offencesJsonObject.isPresent(), is(true));
        assertThat(offencesJsonObject.get().size(), is(2));

        final JsonObject offencesJsonObject1 = offencesJsonObject.get().get(0);
        final JsonObject offencesJsonObject2 = offencesJsonObject.get().get(1);

        assertThat(offencesJsonObject1.getString("cjsOffenceCode"), is(offenceCode1));
        assertThat(offencesJsonObject2.getString("cjsOffenceCode"), is(offenceCode2));

        assertThat(offencesJsonObject1.getString("reportRestrictResultCode"), is("YES"));
        assertThat(offencesJsonObject2.getString("reportRestrictResultCode"), is(EMPTY));

        assertThat(offencesJsonObject1.getString("legislation"), is("Contrary to section 32 of the Sexual Offences Act 2003."));
        assertThat(offencesJsonObject2.getString("legislation"), is("Parent of child of compulsory school age registered at a school who failed to attend regularly"));
    }

    @Test
    public void shouldReturnEmptyJsonObjectWhenOffencesResponsePayloadIsEmpty() {
        final String offenceCode1 = randomAlphanumeric(8);
        final String offenceCode2 = randomAlphanumeric(8);

        final List<String> offenceCodes = Arrays.asList(offenceCode1, offenceCode2);

        final JsonObject responsePayload = Json.createReader(
                new ByteArrayInputStream("{\"offences\":[]}".getBytes()))
                .readObject();

        final JsonEnvelope responseEnvelope = createEnvelope("referencedataoffences.query.offences-list", responsePayload);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder()
                .withId(randomUUID())
                .withName("referencedataoffences.query.offences-list"), JsonValue.NULL);

        when(requester.request(any())).thenReturn(responseEnvelope);

        final Optional<List<JsonObject>> offencesJsonObject = referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(offenceCodes, envelope, requester);

        verify(requester).request(envelopeArgumentCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeArgumentCaptor.getValue();
        MatcherAssert.assertThat(capturedEnvelope.metadata().name(), is("referencedataoffences.query.offences-list"));

        assertThat(offencesJsonObject.isPresent(), is(true));
        assertThat(offencesJsonObject.get().size(), is(0));
    }

    @Test
    public void shouldReturnEmptyJsonObjectWhenOffencesResponsePayloadIsNull() {
        final String offenceCode1 = randomAlphanumeric(8);
        final String offenceCode2 = randomAlphanumeric(8);

        final List<String> offenceCodes = Arrays.asList(offenceCode1, offenceCode2);

        final JsonEnvelope responseEnvelope = createEnvelope("referencedataoffences.query.offences-list", null);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder()
                .withId(randomUUID())
                .withName("referencedataoffences.query.offences-list"), JsonValue.NULL);

        when(requester.request(any())).thenReturn(responseEnvelope);

        final Optional<List<JsonObject>> offencesJsonObject = referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(offenceCodes, envelope, requester);

        verify(requester).request(envelopeArgumentCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeArgumentCaptor.getValue();
        MatcherAssert.assertThat(capturedEnvelope.metadata().name(), is("referencedataoffences.query.offences-list"));


        assertThat(offencesJsonObject.isPresent(), is(false));
    }

    @Test
    public void shouldReturnTitleAndLegislationWhenDocumentIsNotPresent() throws IOException {

        final String offenceCode = randomAlphanumeric(8);

        final JsonEnvelope responseEnvelope = prepareResponseEnvelopeForOffencesList(offenceCode,"/referencedataoffences.offence-without-document.json");

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(DefaultJsonMetadata.metadataBuilder()
                .withId(randomUUID())
                .withName("referencedataoffences.query.offences-list"), JsonValue.NULL);

        when(requester.request(any())).thenReturn(responseEnvelope);

        final Optional<JsonObject> offenceJsonObject = referenceDataOffenceService.getOffenceByCjsCode(offenceCode, envelope, requester);

        verify(requester).request(envelopeArgumentCaptor.capture());

        final DefaultEnvelope capturedEnvelope = envelopeArgumentCaptor.getValue();
        MatcherAssert.assertThat(capturedEnvelope.metadata().name(), is("referencedataoffences.query.offences-list"));

        assertThat(offenceJsonObject.isPresent(), is(true));
        JsonObject offence = offenceJsonObject.get();

        assertThat(offence.getString("title"), is("title"));
        assertThat(offence.getString("legislation"), is("legislation"));
        assertThat(offence.getString("welshoffencetitle"), is("titleWelsh"));
        assertThat(offence.getString("welshlegislation"), is("legislationWelsh"));

    }

    private static JsonEnvelope prepareResponseEnvelopeForOffencesList(final String offenceCode1, final String offenceCode2,final String fileName) throws IOException {
        final String jsonString = givenPayload(fileName).toString()
                .replace("OFFENCE_CODE_1", offenceCode1)
                .replace("OFFENCE_CODE_2", offenceCode2);
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("referencedataoffences.query.offences-list", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonEnvelope prepareResponseEnvelopeForOffencesList(final String offenceCode, final String fileName) throws IOException {
        final String jsonString = givenPayload(fileName).toString()
                .replace("OFFENCE_CODE", offenceCode);
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("referencedataoffences.query.offences-list", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonEnvelope prepareResponseEnvelopeForOffencesListWhichOneOfThemWithoutReportingRestrictions(final String offenceCode1, final String offenceCode2) throws IOException {
        final String jsonString = givenPayload("/referencedataoffences.offences-list-one-of-them-without-rr.json").toString()
                .replace("OFFENCE_CODE_1", offenceCode1)
                .replace("OFFENCE_CODE_2", offenceCode2);
        try {
            final JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
            return createEnvelope("referencedataoffences.query.offences-list", jsonReader.readObject());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

}
