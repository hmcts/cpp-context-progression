package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.FormType.PET;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.CreatePetForm;
import uk.gov.justice.core.courts.FinalisePetForm;
import uk.gov.justice.core.courts.PetDefendants;
import uk.gov.justice.core.courts.PetDetailReceived;
import uk.gov.justice.core.courts.PetDetailUpdated;
import uk.gov.justice.core.courts.PetFormCreated;
import uk.gov.justice.core.courts.PetFormDefendantUpdated;
import uk.gov.justice.core.courts.PetFormFinalised;
import uk.gov.justice.core.courts.PetFormReceived;
import uk.gov.justice.core.courts.PetFormReleased;
import uk.gov.justice.core.courts.PetFormUpdated;
import uk.gov.justice.core.courts.ReceivePetDetail;
import uk.gov.justice.core.courts.ReceivePetForm;
import uk.gov.justice.core.courts.UpdatePetDetail;
import uk.gov.justice.core.courts.UpdatePetForm;
import uk.gov.justice.core.courts.UpdatePetFormForDefendant;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.ReleasePetForm;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PetCommandHandlerTest {
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            PetFormCreated.class, PetFormReceived.class, PetFormUpdated.class, PetFormDefendantUpdated.class, PetFormFinalised.class, PetDetailUpdated.class, PetDetailReceived.class,
            PetFormReleased.class);

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private CaseAggregate caseAggregate;

    @InjectMocks
    @Spy
    private PetCommandHandler petCommandHandler;

    @Before
    public void setup() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
    }

    @Test
    public void shouldHandleCreatePetForm() throws EventStreamException {

        final CreatePetForm createPetForm = CreatePetForm.createPetForm()
                .withPetId(randomUUID())
                .withCaseId(randomUUID())
                .withFormId(randomUUID())
                .withIsYouth(false)
                .withPetFormData("{}")
                .withPetDefendants(asList(PetDefendants.petDefendants()
                        .withDefendantId(randomUUID())
                        .build()))
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.create-pet-form")
                .withId(randomUUID())
                .build();

        final Envelope<CreatePetForm> envelope = envelopeFrom(metadata, createPetForm);

        when(caseAggregate.createPetForm(any(), any(), any(), anyObject(), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(Stream.of(PetFormCreated.petFormCreated()
                        .withCaseId(createPetForm.getCaseId())
                        .withFormId(createPetForm.getFormId())
                        .withPetId(createPetForm.getPetId())
                        .withFormType(PET)
                        .build()));

        petCommandHandler.handleCreatePetForm(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.pet-form-created"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(createPetForm.getCaseId().toString())),
                                withJsonPath("$.formId", is(createPetForm.getFormId().toString())),
                                withJsonPath("$.petId", is(createPetForm.getPetId().toString())))
                        ))
        ));

    }

    @Test
    public void shouldHandleReceivePetForm() throws EventStreamException {

        final ReceivePetForm receivePetForm = ReceivePetForm.receivePetForm()
                .withPetId(randomUUID())
                .withCaseId(randomUUID())
                .withFormId(randomUUID())
                .withPetDefendants(asList(PetDefendants.petDefendants()
                        .withDefendantId(randomUUID())
                        .build()))
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.receive-pet-form")
                .withId(randomUUID())
                .build();

        final Envelope<ReceivePetForm> envelope = envelopeFrom(metadata, receivePetForm);

        when(caseAggregate.receivePetForm(any(), any(), any(), anyList()))
                .thenReturn(Stream.of(PetFormReceived.petFormReceived()
                        .withCaseId(receivePetForm.getCaseId())
                        .withFormId(receivePetForm.getFormId())
                        .withPetId(receivePetForm.getPetId())
                        .build()));

        petCommandHandler.handleReceivePetForm(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.pet-form-received"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(receivePetForm.getCaseId().toString())),
                                withJsonPath("$.formId", is(receivePetForm.getFormId().toString())),
                                withJsonPath("$.petId", is(receivePetForm.getPetId().toString())))
                        ))
        ));

    }

    @Test
    public void shouldHandleUpdatePetForm() throws EventStreamException {

        final UpdatePetForm updatePetForm = UpdatePetForm.updatePetForm()
                .withPetId(randomUUID())
                .withPetFormData("{}")
                .withCaseId(randomUUID())
                .build();

        final UUID userId = randomUUID();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-pet-form")
                .withId(randomUUID())
                .withUserId(userId.toString())
                .build();

        final Envelope<UpdatePetForm> envelope = envelopeFrom(metadata, updatePetForm);

        when(caseAggregate.updatePetForm(any(), anyString(), any(), any()))
                .thenReturn(Stream.of(PetFormUpdated.petFormUpdated()
                        .withCaseId(updatePetForm.getCaseId())
                        .withPetId(updatePetForm.getPetId())
                        .withPetFormData(updatePetForm.getPetFormData())
                        .withUserId(userId)
                        .build()));

        petCommandHandler.handleUpdatePetForm(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.pet-form-updated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(updatePetForm.getCaseId().toString())),
                                withJsonPath("$.petFormData", is(updatePetForm.getPetFormData())),
                                withJsonPath("$.petId", is(updatePetForm.getPetId().toString())),
                                withJsonPath("$.userId", is(userId.toString())))
                        ))
        ));
    }

    @Test
    public void shouldHandleUpdatePetDetail() throws EventStreamException {

        final UpdatePetDetail updatePetDetail = UpdatePetDetail.updatePetDetail()
                .withPetId(randomUUID())
                .withCaseId(randomUUID())
                .withPetDefendants(Arrays.asList(new PetDefendants(randomUUID())))
                .build();

        final UUID userId = randomUUID();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-pet-detail")
                .withId(randomUUID())
                .withUserId(userId.toString())
                .build();

        final Envelope<UpdatePetDetail> envelope = envelopeFrom(metadata, updatePetDetail);

        when(caseAggregate.updatePetDetail(any(), any(), anyObject(), any(), any()))
                .thenReturn(Stream.of(PetDetailUpdated.petDetailUpdated()
                        .withCaseId(updatePetDetail.getCaseId())
                        .withPetId(updatePetDetail.getPetId())
                        .withUserId(userId)
                        .build()));

        petCommandHandler.handleUpdatePetDetail(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.pet-detail-updated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(updatePetDetail.getCaseId().toString())),
                                withJsonPath("$.petId", is(updatePetDetail.getPetId().toString())),
                                withJsonPath("$.userId", is(userId.toString())))
                        ))
        ));
    }

    @Test
    public void shouldHandleReceivePetDetail() throws EventStreamException {

        final ReceivePetDetail receivePetDetail = ReceivePetDetail.receivePetDetail()
                .withPetId(randomUUID())
                .withCaseId(randomUUID())
                .withPetDefendants(Arrays.asList(new PetDefendants(randomUUID())))
                .build();

        final UUID userId = randomUUID();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.receive-pet-detail")
                .withId(randomUUID())
                .withUserId(userId.toString())
                .build();

        final Envelope<ReceivePetDetail> envelope = envelopeFrom(metadata, receivePetDetail);

        when(caseAggregate.receivePetDetail(any(), any(), any(), any()))
                .thenReturn(Stream.of(PetDetailReceived.petDetailReceived()
                        .withCaseId(receivePetDetail.getCaseId())
                        .withPetId(receivePetDetail.getPetId())
                        .withUserId(userId)
                        .build()));

        petCommandHandler.handleReceivePetDetail(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.pet-detail-received"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(receivePetDetail.getCaseId().toString())),
                                withJsonPath("$.petId", is(receivePetDetail.getPetId().toString())),
                                withJsonPath("$.userId", is(userId.toString())))
                        ))
        ));
    }

    @Test
    public void shouldHandleFinalisePetForm() throws EventStreamException {

        final FinalisePetForm finalisePetForm = FinalisePetForm.finalisePetForm()
                .withPetId(randomUUID())
                .withCaseId(randomUUID())
                .build();

        final UUID userId = randomUUID();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.finalise-pet-form")
                .withId(randomUUID())
                .withUserId(userId.toString())
                .build();

        final Envelope<FinalisePetForm> envelope = envelopeFrom(metadata, finalisePetForm);

        when(caseAggregate.finalisePetForm(any(), any(), any(), any()))
                .thenReturn(Stream.of(PetFormFinalised.petFormFinalised()
                        .withCaseId(finalisePetForm.getCaseId())
                        .withPetId(finalisePetForm.getPetId())
                        .withUserId(userId)
                        .build()));

        petCommandHandler.handleFinalisePetForm(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.pet-form-finalised"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(finalisePetForm.getCaseId().toString())),
                                withJsonPath("$.petId", is(finalisePetForm.getPetId().toString())),
                                withJsonPath("$.userId", is(userId.toString())))
                        ))
        ));
    }


    @Test
    public void shouldHandleUpdatePetFormForDefendant() throws EventStreamException {

        final UpdatePetFormForDefendant updatePetFormForDefendant = UpdatePetFormForDefendant.updatePetFormForDefendant()
                .withPetId(randomUUID())
                .withCaseId(randomUUID())
                .withDefendantId(randomUUID())
                .withDefendantData("{}")
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-pet-form-for-defendant")
                .withId(randomUUID())
                .build();

        final Envelope<UpdatePetFormForDefendant> envelope = envelopeFrom(metadata, updatePetFormForDefendant);

        when(caseAggregate.updatePetFormForDefendant(any(), any(), any(), any(), any()))
                .thenReturn(Stream.of(PetFormDefendantUpdated.petFormDefendantUpdated()
                        .withCaseId(updatePetFormForDefendant.getCaseId())
                        .withDefendantId(updatePetFormForDefendant.getDefendantId())
                        .withPetId(updatePetFormForDefendant.getPetId())
                        .withDefendantData(updatePetFormForDefendant.getDefendantData())
                        .build()));

        petCommandHandler.handleUpdatePetFormForDefendant(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.pet-form-defendant-updated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(updatePetFormForDefendant.getCaseId().toString())),
                                withJsonPath("$.defendantId", is(updatePetFormForDefendant.getDefendantId().toString())),
                                withJsonPath("$.petId", is(updatePetFormForDefendant.getPetId().toString())))
                        ))
        ));

    }

    @Test
    public void shouldHandleReleasePetForm() throws EventStreamException {

        final ReleasePetForm releasePetForm = ReleasePetForm.releasePetForm()
                .withPetId(randomUUID())
                .withCaseId(randomUUID())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.release-pet-form")
                .withId(randomUUID())
                .build();

        final Envelope<ReleasePetForm> envelope = envelopeFrom(metadata, releasePetForm);

        when(caseAggregate.releasePetForm(any(), any(), any()))
                .thenReturn(Stream.of(PetFormReleased.petFormReleased()
                        .withCaseId(releasePetForm.getCaseId())
                        .withPetId(releasePetForm.getPetId())
                        .build()));

        petCommandHandler.handleReleasePetForm(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.pet-form-released"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(releasePetForm.getCaseId().toString())),
                                withJsonPath("$.petId", is(releasePetForm.getPetId().toString())))
                        ))
        ));
    }
}