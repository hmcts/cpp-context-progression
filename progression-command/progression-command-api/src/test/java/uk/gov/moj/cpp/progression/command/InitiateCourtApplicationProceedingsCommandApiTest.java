package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InitiateCourtApplicationProceedingsCommandApiTest {

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @InjectMocks
    private InitiateCourtApplicationProceedingsCommandApi initiateCourtApplicationProceedingsCommandApi;

    @Test
    public void shouldInitialCourtProceedingsForCourtApplicationWhenNoApplicationReferenceSet() {
        final JsonEnvelope commandEnvelope = buildEnvelope();

        final Envelope queryResponseEnvelope = mock(Envelope.class);
        when(queryResponseEnvelope.payload()).thenReturn(createObjectBuilder().add("hasPermission", true).build());
        when(requester.request(any(), any())).thenReturn(queryResponseEnvelope);
        stubEmptyPermissions();


        initiateCourtApplicationProceedingsCommandApi.initiateCourtApplicationProceedings(commandEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final DefaultEnvelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is("progression.command.initiate-court-proceedings-for-application"));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    @Test
    public void shouldInitialCourtProceedingsForCourtApplicationWhenApplicationReferenceIsValid() {
        final String validURN = "ASD1RTY5WE1";//11 Char length, alfaNumeric, all upper case
        final JsonObject payload = createObjectBuilder()
                .add("courtApplication", createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("type", createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add("code", "anyCode")
                                .add("linkType", "STANDALONE"))
                        .add("applicationReference", validURN)
                        .build())
                .build();

        final JsonEnvelope commandEnvelope = buildEnvelope(payload);

        final Envelope queryResponseEnvelope = mock(Envelope.class);
        when(queryResponseEnvelope.payload()).thenReturn(createObjectBuilder().add("hasPermission", true).build());
        when(requester.request(any(), any())).thenReturn(queryResponseEnvelope);
        stubEmptyPermissions();


        initiateCourtApplicationProceedingsCommandApi.initiateCourtApplicationProceedings(commandEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final DefaultEnvelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is("progression.command.initiate-court-proceedings-for-application"));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    @Test
    public void shouldValidationFailOnInitialCourtProceedingsForCourtApplicationWhenApplicationReferenceIsEmpty() {
        final String invalidURN = "";
        final JsonObject payload = createObjectBuilder()
                .add("courtApplication", createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("type", createObjectBuilder()
                                .add("code", "anyCode")
                                .add("linkType", "STANDALONE"))
                        .add("applicationReference", invalidURN)
                        .build())
                .build();

        final JsonEnvelope commandEnvelope = buildEnvelope(payload);

        final Envelope queryResponseEnvelope = mock(Envelope.class);
        when(queryResponseEnvelope.payload()).thenReturn(createObjectBuilder().add("hasPermission", true).build());
        when(requester.request(any(), any())).thenReturn(queryResponseEnvelope);

        assertThrows(BadRequestException.class, () -> initiateCourtApplicationProceedingsCommandApi.initiateCourtApplicationProceedings(commandEnvelope));

    }

    @Test
    public void shouldValidationFailOnInitialCourtProceedingsForCourtApplicationWhenApplicationReferenceIsNineCharLength() {
        final String invalidURN = "ASDERTYUW";//NOT 11 Char length, alfaNumeric, all upper case
        final JsonObject payload = createObjectBuilder()
                .add("courtApplication", createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("type", createObjectBuilder()
                                .add("code", "anyCode")
                                .add("linkType", "STANDALONE"))
                        .add("applicationReference", invalidURN)
                        .build())
                .build();

        final JsonEnvelope commandEnvelope = buildEnvelope(payload);

        final Envelope queryResponseEnvelope = mock(Envelope.class);
        when(queryResponseEnvelope.payload()).thenReturn(createObjectBuilder().add("hasPermission", true).build());
        when(requester.request(any(), any())).thenReturn(queryResponseEnvelope);

        assertThrows(BadRequestException.class, () -> initiateCourtApplicationProceedingsCommandApi.initiateCourtApplicationProceedings(commandEnvelope));

    }

    @Test
    public void shouldValidationFailOnInitialCourtProceedingsForCourtApplicationWhenApplicationReferenceIsNotAllCapital() {
        final String invalidURN = "ASDERTYUWXe";//11 Char length, alfaNumeric, NOT all upper case
        final JsonObject payload = createObjectBuilder()
                .add("courtApplication", createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("type", createObjectBuilder()
                                .add("linkType", "STANDALONE")
                                .add("code", "anyCode"))
                        .add("applicationReference", invalidURN)
                        .build())
                .build();

        final JsonEnvelope commandEnvelope = buildEnvelope(payload);

        final Envelope queryResponseEnvelope = mock(Envelope.class);
        when(queryResponseEnvelope.payload()).thenReturn(createObjectBuilder().add("hasPermission", true).build());
        when(requester.request(any(), any())).thenReturn(queryResponseEnvelope);

        assertThrows(BadRequestException.class, () -> initiateCourtApplicationProceedingsCommandApi.initiateCourtApplicationProceedings(commandEnvelope));

    }

    @Test
    public void shouldValidationFailOnInitialCourtProceedingsForCourtApplicationWhenApplicationReferenceIsNotAllAlfaNumeric() {
        final String invalidURN = "ASD!RTYU1EX";//11 Char length, NOT all alfaNumeric, all upper case
        final JsonObject payload = createObjectBuilder()
                .add("courtApplication", createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("type", createObjectBuilder()
                                .add("code", "anyCode")
                                .add("linkType", "STANDALONE"))
                        .add("applicationReference", invalidURN)
                        .build())
                .build();

        final JsonEnvelope commandEnvelope = buildEnvelope(payload);

        final Envelope queryResponseEnvelope = mock(Envelope.class);
        when(queryResponseEnvelope.payload()).thenReturn(createObjectBuilder().add("hasPermission", true).build());
        when(requester.request(any(), any())).thenReturn(queryResponseEnvelope);

        assertThrows(BadRequestException.class, () -> initiateCourtApplicationProceedingsCommandApi.initiateCourtApplicationProceedings(commandEnvelope));

    }

    @Test
    public void shouldThrowForbiddenRequestExceptionForInitialCourtProceedingsForCourtApplicationWhenUserNotAuthorisedForTheApplicationType() {
        final JsonEnvelope commandEnvelope = buildEnvelope();

        final Envelope queryResponseEnvelope = mock(Envelope.class);
        when(queryResponseEnvelope.payload()).thenReturn(createObjectBuilder().add("hasPermission", false).build());
        when(requester.request(any(), any())).thenReturn(queryResponseEnvelope);

        assertThrows(ForbiddenRequestException.class, () -> initiateCourtApplicationProceedingsCommandApi.initiateCourtApplicationProceedings(commandEnvelope));
    }

    @Test
    public void shouldEditCourtProceedingsForCourtApplication() {
        final JsonEnvelope commandEnvelope = buildEnvelope();

        initiateCourtApplicationProceedingsCommandApi.editCourtApplicationProceedings(commandEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final DefaultEnvelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is("progression.command.edit-court-proceedings-for-application"));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    @Test
    public void shouldCallAddBreachApplication() {
        final JsonEnvelope commandEnvelope = buildEnvelope();

        initiateCourtApplicationProceedingsCommandApi.addBreachApplication(commandEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final DefaultEnvelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is("progression.command.add-breach-application"));
        assertThat(newCommand.payload(), equalTo(commandEnvelope.payloadAsJsonObject()));
    }

    @Test
    public void shouldSendCommandWhenSubmittedHearingTypeIsAllowed() {
        final String applicationTypeId = randomUUID().toString();
        final String hearingTypeId = randomUUID().toString();
        final JsonEnvelope commandEnvelope = buildStandaloneEnvelope(applicationTypeId, hearingTypeId);

        stubHasPermission(true);
        stubPermissions(allowedHearingTypePermissions(applicationTypeId, hearingTypeId));

        initiateCourtApplicationProceedingsCommandApi.initiateCourtApplicationProceedings(commandEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is("progression.command.initiate-court-proceedings-for-application"));
    }

    @Test
    public void shouldSendCommandWhenSubmittedHearingTypeIsOneOfMultipleAllowed() {
        final String applicationTypeId = randomUUID().toString();
        final String submittedHearingTypeId = randomUUID().toString();
        final JsonEnvelope commandEnvelope = buildStandaloneEnvelope(applicationTypeId, submittedHearingTypeId);

        stubHasPermission(true);
        stubPermissions(allowedHearingTypePermissions(applicationTypeId,
                randomUUID().toString(), submittedHearingTypeId, randomUUID().toString()));

        initiateCourtApplicationProceedingsCommandApi.initiateCourtApplicationProceedings(commandEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is("progression.command.initiate-court-proceedings-for-application"));
    }

    @Test
    public void shouldThrowBadRequestWhenSubmittedHearingTypeIsNotAllowed() {
        final String applicationTypeId = randomUUID().toString();
        final JsonEnvelope commandEnvelope = buildStandaloneEnvelope(applicationTypeId, randomUUID().toString());

        stubHasPermission(true);
        stubPermissions(allowedHearingTypePermissions(applicationTypeId, randomUUID().toString(), randomUUID().toString()));

        assertThrows(BadRequestException.class, () -> initiateCourtApplicationProceedingsCommandApi.initiateCourtApplicationProceedings(commandEnvelope));
    }

    @Test
    public void shouldThrowBadRequestWhenHearingTypeAbsentButAllowedHearingTypesExist() {
        final String applicationTypeId = randomUUID().toString();
        final JsonEnvelope commandEnvelope = buildStandaloneEnvelope(applicationTypeId, null);

        stubHasPermission(true);
        stubPermissions(allowedHearingTypePermissions(applicationTypeId, randomUUID().toString()));

        assertThrows(BadRequestException.class, () -> initiateCourtApplicationProceedingsCommandApi.initiateCourtApplicationProceedings(commandEnvelope));
    }

    @Test
    public void shouldSendCommandWhenBoxHearingAndAllowedHearingTypesExist() {
        final String applicationTypeId = randomUUID().toString();
        final JsonEnvelope commandEnvelope = buildStandaloneBoxHearingEnvelope(applicationTypeId);

        stubHasPermission(true);

        initiateCourtApplicationProceedingsCommandApi.initiateCourtApplicationProceedings(commandEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is("progression.command.initiate-court-proceedings-for-application"));
    }

    @Test
    public void shouldSendCommandWhenNoAllowedHearingTypeMappingExists() {
        final String applicationTypeId = randomUUID().toString();
        final JsonEnvelope commandEnvelope = buildStandaloneEnvelope(applicationTypeId, randomUUID().toString());

        stubHasPermission(true);
        stubEmptyPermissions();

        initiateCourtApplicationProceedingsCommandApi.initiateCourtApplicationProceedings(commandEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is("progression.command.initiate-court-proceedings-for-application"));
    }

    @Test
    public void shouldNotEnforceHearingTypeForNonStandaloneApplication() {
        final JsonObject payload = createObjectBuilder()
                .add("courtApplication", createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("type", createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add("code", "anyCode")
                                .add("linkType", "LINKED"))
                        .build())
                .build();
        final JsonEnvelope commandEnvelope = buildEnvelope(payload);

        stubHasPermission(true);

        initiateCourtApplicationProceedingsCommandApi.initiateCourtApplicationProceedings(commandEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is("progression.command.initiate-court-proceedings-for-application"));
    }

    private JsonEnvelope buildStandaloneEnvelope(final String applicationTypeId, final String hearingTypeId) {
        final JsonObjectBuilder courtApplication = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("type", createObjectBuilder()
                        .add("id", applicationTypeId)
                        .add("code", "anyCode")
                        .add("linkType", "STANDALONE"));
        final JsonObjectBuilder payload = createObjectBuilder().add("courtApplication", courtApplication);
        if (hearingTypeId != null) {
            payload.add("courtHearing", createObjectBuilder()
                    .add("hearingType", createObjectBuilder().add("id", hearingTypeId)));
        }
        return buildEnvelope(payload.build());
    }

    private JsonEnvelope buildStandaloneBoxHearingEnvelope(final String applicationTypeId) {
        final JsonObject payload = createObjectBuilder()
                .add("courtApplication", createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("type", createObjectBuilder()
                                .add("id", applicationTypeId)
                                .add("code", "anyCode")
                                .add("linkType", "STANDALONE")))
                .add("boxHearing", createObjectBuilder().add("jurisdictionType", "MAGISTRATES"))
                .build();
        return buildEnvelope(payload);
    }

    private JsonObject allowedHearingTypePermissions(final String source, final String... targets) {
        final JsonArrayBuilder permissions = createArrayBuilder();
        for (final String target : targets) {
            permissions.add(createObjectBuilder()
                    .add("object", "HearingType")
                    .add("action", "Locked")
                    .add("active", true)
                    .add("source", source)
                    .add("target", target));
        }
        return createObjectBuilder().add("permissions", permissions).build();
    }

    private void stubHasPermission(final boolean value) {
        final Envelope queryResponseEnvelope = mock(Envelope.class);
        when(queryResponseEnvelope.payload()).thenReturn(createObjectBuilder().add("hasPermission", value).build());
        when(requester.request(any(), any())).thenReturn(queryResponseEnvelope);
    }

    private void stubPermissions(final JsonObject permissionsPayload) {
        final Envelope permissionsEnvelope = mock(Envelope.class);
        when(permissionsEnvelope.payload()).thenReturn(permissionsPayload);
        when(requester.requestAsAdmin(any(), any())).thenReturn(permissionsEnvelope);
    }

    private void stubEmptyPermissions() {
        stubPermissions(createObjectBuilder().build());
    }

    private JsonEnvelope buildEnvelope() {
        final JsonObject payload = createObjectBuilder()
                .add("courtApplication", createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("type", createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add("code", "anyCode")
                                .add("linkType", "STANDALONE"))
                        .build())
                .build();

        return buildEnvelope(payload);
    }

    private JsonEnvelope buildEnvelope(final JsonObject payload) {

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }
}
