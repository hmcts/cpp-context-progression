package uk.gov.moj.cpp.progression.processor;

import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.ApplicationOffencesUpdated;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.progression.query.laa.HearingSummary;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;

import javax.json.JsonObject;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;


@ExtendWith(MockitoExtension.class)
public class ApplicationOffencesUpdatedProcessorTest {

    static final String PUBLIC_PROGRESSION_APPLICATION_OFFENCES_UPDATED = "public.progression.application-offences-updated";
    static final String PRIVATE_COMMAND_PROGRESSION_UPDATE_LAA_REFERENCE_FOR_HEARING_ = "progression.command.update-application-laa-reference-for-hearing";

    @InjectMocks
    private ApplicationOffencesUpdatedProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope finalEnvelopeForPublicEvent;

    @Mock
    private JsonEnvelope finalEnvelopeForCommandHandler;

    @Mock
    private Function<Object, JsonEnvelope> publicEventEnveloperFunction;

    @Mock
    private Function<Object, JsonEnvelope> commandHandlerEnveloperFunction;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private CourtApplicationRepository courtApplicationRepository;

    @Spy
    private ListToJsonArrayConverter<?> jsonConverter;

    @Mock
    private ProgressionService progressionService;

    @BeforeEach
    public void initMocks() {

        setField(this.jsonConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter", new JsonObjectConvertersFactory().stringToJsonObjectConverter());

        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldHandleApplicationOffencesUpdatedEventMessage(){
        UUID applicationId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        LaaReference laaReference = LaaReference.laaReference().withApplicationReference("applicationReference")
                .withStatusCode("statusCode").withStatusDescription("description").build();

        ApplicationOffencesUpdated applicationOffencesUpdated = ApplicationOffencesUpdated.applicationOffencesUpdated()
                .withApplicationId(applicationId)
                .withSubjectId(subjectId)
                .withOffenceId(offenceId)
                .withLaaReference(laaReference)
                .build();

        final CourtApplicationEntity applicationEntity = new CourtApplicationEntity();
        applicationEntity.setApplicationId(applicationId);
        applicationEntity.setPayload(payload.toString());
        HearingSummary hearingSummary1 = HearingSummary.hearingSummary().withHearingId(UUID.randomUUID()).build();
        HearingSummary hearingSummary2 = HearingSummary.hearingSummary().withHearingId(UUID.randomUUID()).build();
        when(jsonObjectToObjectConverter.convert(payload, ApplicationOffencesUpdated.class)).thenReturn(applicationOffencesUpdated);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(applicationEntity);
        when(enveloper.withMetadataFrom(envelope, PUBLIC_PROGRESSION_APPLICATION_OFFENCES_UPDATED)).thenReturn(publicEventEnveloperFunction);
        when(enveloper.withMetadataFrom(envelope, PRIVATE_COMMAND_PROGRESSION_UPDATE_LAA_REFERENCE_FOR_HEARING_)).thenReturn(commandHandlerEnveloperFunction);
        when(publicEventEnveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelopeForPublicEvent);
        when(commandHandlerEnveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelopeForCommandHandler);
        when(progressionService.getHearingsForApplication(applicationId)).thenReturn(Optional.of(List.of(hearingSummary1, hearingSummary2)));
        final JsonObject applicationJson = createObjectBuilder().build();
        when(stringToJsonObjectConverter.convert(applicationEntity.getPayload())).thenReturn(applicationJson);
        CourtApplication persistedApplication = CourtApplication.courtApplication()
                .withSubject(CourtApplicationParty.courtApplicationParty().withId(subjectId).build())
                .withCourtApplicationCases(buildCourtApplicationCases(offenceId))
                .build();
        when(jsonObjectToObjectConverter.convert(applicationJson, CourtApplication.class)).thenReturn(persistedApplication);
        when(objectToJsonObjectConverter.convert(applicationOffencesUpdated.getLaaReference())).thenReturn(jsonObject);

        eventProcessor.handleApplicationOffencesUpdatedEvent(envelope);

        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(3)).send(captor.capture());
        verify(sender).send(finalEnvelopeForPublicEvent);
        verify(sender, times(2)).send(finalEnvelopeForCommandHandler);
    }

    @Test
    public void shouldNotRaisePublicEventWhenApplicationIsNotFound(){
        UUID applicationId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        LaaReference laaReference = LaaReference.laaReference().withApplicationReference("applicationReference")
                .withStatusCode("statusCode").withStatusDescription("description").build();

        ApplicationOffencesUpdated applicationOffencesUpdated = ApplicationOffencesUpdated.applicationOffencesUpdated()
                .withApplicationId(applicationId)
                .withSubjectId(subjectId)
                .withOffenceId(offenceId)
                .withLaaReference(laaReference)
                .build();

        final CourtApplicationEntity applicationEntity = new CourtApplicationEntity();
        applicationEntity.setApplicationId(UUID.randomUUID());
        applicationEntity.setPayload(payload.toString());
        when(jsonObjectToObjectConverter.convert(payload, ApplicationOffencesUpdated.class)).thenReturn(applicationOffencesUpdated);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(null);

        eventProcessor.handleApplicationOffencesUpdatedEvent(envelope);
        verifyNoInteractions(sender);

    }

    @Test
    public void shouldNotRaisePublicEventWhenSubjectIdIsNotMatched(){
        UUID applicationId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        LaaReference laaReference = LaaReference.laaReference().withApplicationReference("applicationReference")
                .withStatusCode("statusCode").withStatusDescription("description").build();

        ApplicationOffencesUpdated applicationOffencesUpdated = ApplicationOffencesUpdated.applicationOffencesUpdated()
                .withApplicationId(applicationId)
                .withSubjectId(subjectId)
                .withOffenceId(offenceId)
                .withLaaReference(laaReference)
                .build();

        final CourtApplicationEntity applicationEntity = new CourtApplicationEntity();
        applicationEntity.setApplicationId(applicationId);
        applicationEntity.setPayload(payload.toString());
        when(jsonObjectToObjectConverter.convert(payload, ApplicationOffencesUpdated.class)).thenReturn(applicationOffencesUpdated);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(applicationEntity);

        final JsonObject applicationJson = createObjectBuilder().build();
        when(stringToJsonObjectConverter.convert(applicationEntity.getPayload())).thenReturn(applicationJson);
        CourtApplication persistedApplication = CourtApplication.courtApplication()
                .withSubject(CourtApplicationParty.courtApplicationParty().withId(UUID.randomUUID()).build())
                .withCourtApplicationCases(buildCourtApplicationCases(offenceId))
                .build();
        when(jsonObjectToObjectConverter.convert(applicationJson, CourtApplication.class)).thenReturn(persistedApplication);

        eventProcessor.handleApplicationOffencesUpdatedEvent(envelope);
        verifyNoInteractions(sender);
    }

    @Test
    public void shouldNotRaisePublicEventWhenOffenceIdIsNotMatched(){
        UUID applicationId = UUID.randomUUID();
        UUID offenceId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        LaaReference laaReference = LaaReference.laaReference().withApplicationReference("applicationReference")
                .withStatusCode("statusCode").withStatusDescription("description").build();

        ApplicationOffencesUpdated applicationOffencesUpdated = ApplicationOffencesUpdated.applicationOffencesUpdated()
                .withApplicationId(applicationId)
                .withSubjectId(subjectId)
                .withOffenceId(offenceId)
                .withLaaReference(laaReference)
                .build();

        final CourtApplicationEntity applicationEntity = new CourtApplicationEntity();
        applicationEntity.setApplicationId(applicationId);
        applicationEntity.setPayload(payload.toString());
        when(jsonObjectToObjectConverter.convert(payload, ApplicationOffencesUpdated.class)).thenReturn(applicationOffencesUpdated);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(applicationEntity);

        final JsonObject applicationJson = createObjectBuilder().build();
        when(stringToJsonObjectConverter.convert(applicationEntity.getPayload())).thenReturn(applicationJson);
        CourtApplication persistedApplication = CourtApplication.courtApplication()
                .withSubject(CourtApplicationParty.courtApplicationParty().withId(subjectId).build())
                .withCourtApplicationCases(buildCourtApplicationCases(UUID.randomUUID()))
                .build();
        when(jsonObjectToObjectConverter.convert(applicationJson, CourtApplication.class)).thenReturn(persistedApplication);

        eventProcessor.handleApplicationOffencesUpdatedEvent(envelope);
        verifyNoInteractions(sender);
    }

    private List<CourtApplicationCase> buildCourtApplicationCases(UUID offenceId){
        Offence offence1 = Offence.offence().withId(offenceId).withLaaApplnReference(LaaReference.laaReference().withStatusCode("G2").build()).build();
        Offence offence2 = Offence.offence().withId(UUID.randomUUID()).withLaaApplnReference(LaaReference.laaReference().withStatusCode("G2").build()).build();
        Offence offence3 = Offence.offence().withId(UUID.randomUUID()).withLaaApplnReference(LaaReference.laaReference().withStatusCode("G2").build()).build();
        Offence offence4 = Offence.offence().withId(UUID.randomUUID()).withLaaApplnReference(LaaReference.laaReference().withStatusCode("G2").build()).build();

        CourtApplicationCase courtApplicationCase1 =CourtApplicationCase.courtApplicationCase().withOffences(List.of(offence1, offence2)).build();
        CourtApplicationCase courtApplicationCase2 =CourtApplicationCase.courtApplicationCase().withOffences(List.of(offence3)).build();
        CourtApplicationCase courtApplicationCase3 =CourtApplicationCase.courtApplicationCase().withOffences(List.of(offence4)).build();
        return List.of(courtApplicationCase1, courtApplicationCase2, courtApplicationCase3);
    }


}