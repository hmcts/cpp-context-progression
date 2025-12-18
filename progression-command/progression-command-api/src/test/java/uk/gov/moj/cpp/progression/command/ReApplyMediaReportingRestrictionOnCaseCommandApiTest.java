package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.command.service.OrganisationService;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReApplyMediaReportingRestrictionOnCaseCommandApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Enveloper enveloper;

    @Captor
    ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @InjectMocks
    private ReApplyMediaReportingRestrictionOnCaseCommandApi reApplyMediaReportingRestrictionOnCaseCommandApi;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Mock
    OrganisationService organisationService;

    @Mock
    Requester requester;

    @Test
    public void testHandleReApplyMediaReportingRestrictionOnCase() {
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        final JsonObject commandPayload = JsonObjects.createObjectBuilder()
                .add("caseIds", createArrayBuilder()
                        .add(caseId1.toString())
                        .add(caseId2.toString())
                        .build())
                .build();
        final Metadata commandMetadata = metadataBuilder().withName("progression.command.reapply-media-reporting-restrictions")
                .withClientCorrelationId(randomUUID().toString())
                .withId(randomUUID())
                .build();
        final JsonEnvelope commandEnvelope = envelopeFrom(commandMetadata, commandPayload);

        reApplyMediaReportingRestrictionOnCaseCommandApi.handle(commandEnvelope);

        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(2)).send(captor.capture());

        List<Envelope> currentEvents = captor.getAllValues();
        MatcherAssert.assertThat(currentEvents.get(0).metadata().name(), Matchers.is("progression.command.handler.reapply-media-reporting-restrictions"));
        MatcherAssert.assertThat(currentEvents.get(0).payload().toString(), containsString(caseId1.toString()));
    }
}
