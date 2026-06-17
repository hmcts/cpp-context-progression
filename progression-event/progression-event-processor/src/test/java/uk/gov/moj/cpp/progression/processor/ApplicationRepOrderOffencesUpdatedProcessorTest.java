package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ApplicationReporderOffencesUpdated;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.OffencesForDefendantChanged;
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
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.application.ApplicationCaseDefendantOrganisation;
import uk.gov.moj.cpp.progression.event.ApplicationRepOrderUpdatedForApplication;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

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


@ExtendWith(MockitoExtension.class)
public class ApplicationRepOrderOffencesUpdatedProcessorTest {

    @InjectMocks
    private ApplicationRepOrderOffencesUpdatedProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private JsonEnvelope finalEnvelopeForCommand;
    @Mock
    private JsonEnvelope finalEnvelopeForDefence;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;
    @Mock
    private Function<Object, JsonEnvelope> enveloperFunctionForCommand;
    @Mock
    private Function<Object, JsonEnvelope> enveloperFunctionForDefence;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private ObjectToJsonObjectConverter objectToJsonConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private CourtApplicationRepository courtApplicationRepository;

    @Spy
    private ListToJsonArrayConverter<?> jsonConverter;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @BeforeEach
    public void initMocks() {

        setField(this.jsonConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonConverter, "stringToJsonObjectConverter", new JsonObjectConvertersFactory().stringToJsonObjectConverter());

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleApplicationOffencesUpdatedEventMessage() {
        final UUID applicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID subjectId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID organisationId = randomUUID();
        final LaaReference laaReference = LaaReference.laaReference().withApplicationReference("applicationReference")
                .withStatusCode("statusCode").withStatusDescription("description").build();

        final List<ApplicationCaseDefendantOrganisation> applicationCaseDefendantOrganisationList = new ArrayList<>();
        final ApplicationCaseDefendantOrganisation applicationCaseDefendantOrganisation = ApplicationCaseDefendantOrganisation.applicationCaseDefendantOrganisation()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withOrganisationId(organisationId)
                .build();

        applicationCaseDefendantOrganisationList.add(applicationCaseDefendantOrganisation);
        final ApplicationReporderOffencesUpdated applicationOffencesUpdated = ApplicationReporderOffencesUpdated.applicationReporderOffencesUpdated()
                .withApplicationId(applicationId)
                .withSubjectId(subjectId)
                .withOffenceId(offenceId)
                .withLaaReference(laaReference)
                .withApplicationCaseDefendantOrganisations(applicationCaseDefendantOrganisationList)
                .build();

        final JsonObject payload = objectToJsonConverter.convert(applicationOffencesUpdated);

        final CourtApplicationEntity applicationEntity = new CourtApplicationEntity();
        applicationEntity.setApplicationId(applicationId);
        applicationEntity.setPayload(payload.toString());

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadataBuilder()
                .withId(randomUUID())
                .withName("progression.event.application-reporder-offences-updated").build());
        when(jsonObjectToObjectConverter.convert(payload, ApplicationReporderOffencesUpdated.class)).thenReturn(applicationOffencesUpdated);
        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(applicationEntity);
        final JsonObject applicationJson = createObjectBuilder().build();
        when(stringToJsonObjectConverter.convert(applicationEntity.getPayload())).thenReturn(applicationJson);
        DefendantCase defendantCase = DefendantCase.defendantCase()
                .withDefendantId(defendantId)
                .withCaseId(caseId)
                .build();
        List<DefendantCase> defendantCases = new ArrayList<>();
        defendantCases.add(defendantCase);
        CourtApplication persistedApplication = CourtApplication.courtApplication()
                .withSubject(CourtApplicationParty.courtApplicationParty()
                        .withId(subjectId)
                        .withMasterDefendant(MasterDefendant.masterDefendant()
                                .withDefendantCase(defendantCases)
                                .build())
                        .build())
                .withCourtApplicationCases(buildCourtApplicationCases(offenceId, caseId))
                .build();
        when(jsonObjectToObjectConverter.convert(applicationJson, CourtApplication.class)).thenReturn(persistedApplication);
        HearingSummary hearingSummary = HearingSummary.hearingSummary()
                .withHearingId(hearingId)
                .build();
        List<HearingSummary> hearingSummaryList = new ArrayList<>();
        hearingSummaryList.add(hearingSummary);
        when(progressionService.getHearingsForApplication(applicationId)).thenReturn(Optional.of(hearingSummaryList));
        final JsonObject offencesForDefendantChangedJson = createObjectBuilder().build();
        when(objectToJsonObjectConverter.convert(any(OffencesForDefendantChanged.class))).thenReturn(offencesForDefendantChangedJson);

        eventProcessor.handleApplicationOffencesUpdatedEvent(envelope);
        verify(sender, times(3)).send(envelopeCaptor.capture());
        final List<Envelope<JsonObject>> publicEvent = envelopeCaptor.getAllValues();

        assertThat(publicEvent.get(0).metadata().name(), is("public.progression.application-offences-updated"));
        assertThat(publicEvent.get(0).payload().getJsonObject("applicationCaseDefendantOrganisations"), nullValue());
        assertThat(publicEvent.get(0).payload().getString("applicationId"), is(applicationId.toString()));
        assertThat(publicEvent.get(0).payload().getString("subjectId"), is(subjectId.toString()));
        assertThat(publicEvent.get(0).payload().getString("offenceId"), is(offenceId.toString()));
        assertThat(publicEvent.get(0).payload().getJsonObject("laaReference"), is(objectToJsonConverter.convert(laaReference)));
        assertThat(publicEvent.get(1).metadata().name(), is("progression.command.update-application-laa-reference-for-hearing"));
        assertThat(publicEvent.get(1).payload().getString("applicationId"), is(applicationId.toString()));
        assertThat(publicEvent.get(1).payload().getString("subjectId"), is(subjectId.toString()));
        assertThat(publicEvent.get(1).payload().getString("offenceId"), is(offenceId.toString()));
        assertThat(publicEvent.get(1).payload().getString("hearingId"), is(hearingId.toString()));
        assertThat(publicEvent.get(1).payload().getJsonObject("laaReference"), is(objectToJsonConverter.convert(laaReference)));
        assertThat(publicEvent.get(2).metadata().name(), is("public.progression.defendant-offences-changed"));
    }

    @Test
    public void shouldHandleApplicationRepOrderUpdatedForApplicationEventMessage() {
        final UUID applicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID subjectId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID organisationId = randomUUID();
        final LaaReference laaReference = LaaReference.laaReference()
                .withApplicationReference("applicationReference")
                .withStatusCode("statusCode")
                .withStatusDescription("description")
                .build();

        final ApplicationCaseDefendantOrganisation applicationCaseDefendantOrganisation = ApplicationCaseDefendantOrganisation.applicationCaseDefendantOrganisation()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withOrganisationId(organisationId)
                .build();

        final List<ApplicationCaseDefendantOrganisation> applicationCaseDefendantOrganisationList = new ArrayList<>();
        applicationCaseDefendantOrganisationList.add(applicationCaseDefendantOrganisation);

        final ApplicationRepOrderUpdatedForApplication applicationRepOrderUpdatedForApplication = ApplicationRepOrderUpdatedForApplication.applicationRepOrderUpdatedForApplication()
                .withApplicationId(applicationId)
                .withSubjectId(subjectId)
                .withLaaReference(laaReference)
                .withApplicationCaseDefendantOrganisations(applicationCaseDefendantOrganisationList)
                .build();

        final JsonObject payload = objectToJsonConverter.convert(applicationRepOrderUpdatedForApplication);

        final CourtApplicationEntity applicationEntity = new CourtApplicationEntity();
        applicationEntity.setApplicationId(applicationId);
        applicationEntity.setPayload(payload.toString());

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadataBuilder()
                .withId(randomUUID())
                .withName("progression.event.application-rep-order-updated-for-application").build());
        when(jsonObjectToObjectConverter.convert(payload, ApplicationRepOrderUpdatedForApplication.class)).thenReturn(applicationRepOrderUpdatedForApplication);
        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(applicationEntity);
        final JsonObject applicationJson = createObjectBuilder().build();
        when(stringToJsonObjectConverter.convert(applicationEntity.getPayload())).thenReturn(applicationJson);
        final DefendantCase defendantCase = DefendantCase.defendantCase()
                .withDefendantId(defendantId)
                .withCaseId(caseId)
                .build();
        List<DefendantCase> defendantCases = new ArrayList<>();
        defendantCases.add(defendantCase);
        final CourtApplication persistedApplication = CourtApplication.courtApplication()
                .withSubject(CourtApplicationParty.courtApplicationParty()
                        .withId(subjectId)
                        .withMasterDefendant(MasterDefendant.masterDefendant()
                                .withDefendantCase(defendantCases)
                                .build())
                        .build())
                .build();
        when(jsonObjectToObjectConverter.convert(applicationJson, CourtApplication.class)).thenReturn(persistedApplication);
        HearingSummary hearingSummary = HearingSummary.hearingSummary()
                .withHearingId(hearingId)
                .build();
        List<HearingSummary> hearingSummaryList = new ArrayList<>();
        hearingSummaryList.add(hearingSummary);
        when(progressionService.getHearingsForApplication(applicationId)).thenReturn(Optional.of(hearingSummaryList));
        final JsonObject offencesForDefendantChangedJson = createObjectBuilder().build();
        when(objectToJsonObjectConverter.convert(any(OffencesForDefendantChanged.class))).thenReturn(offencesForDefendantChangedJson);

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(List.of(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(Arrays.asList(
                                Offence.offence()
                                        .withId(offenceId)
                                        .build(),
                                Offence.offence()
                                        .withId(randomUUID())
                                        .build()
                        ))
                        .build()))
                .build();

        when(progressionService.getProsecutionCaseById(any(JsonEnvelope.class), eq(defendantCase.getCaseId().toString()))).thenReturn(JsonObjects.createObjectBuilder().add("prosecutionCase", objectToJsonConverter.convert(prosecutionCase)).build());
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(ProsecutionCase.class))).thenReturn(prosecutionCase);

        eventProcessor.handleApplicationRepOrderUpdatedForApplicationEvent(envelope);
        verify(sender, times(3)).send(envelopeCaptor.capture());
        final List<Envelope<JsonObject>> publicEvent = envelopeCaptor.getAllValues();

        assertThat(publicEvent.get(0).metadata().name(), is("public.progression.application-laa-reference-updated-for-application"));
        assertThat(publicEvent.get(0).payload().getJsonObject("applicationCaseDefendantOrganisations"), nullValue());
        assertThat(publicEvent.get(0).payload().getString("applicationId"), is(applicationId.toString()));
        assertThat(publicEvent.get(0).payload().getString("subjectId"), is(subjectId.toString()));
        assertThat(publicEvent.get(0).payload().getJsonObject("offenceId"), nullValue());
        assertThat(publicEvent.get(0).payload().getJsonObject("laaReference"), is(objectToJsonConverter.convert(laaReference)));
        assertThat(publicEvent.get(1).metadata().name(), is("progression.command.update-application-laa-reference-for-hearing"));
        assertThat(publicEvent.get(1).payload().getString("applicationId"), is(applicationId.toString()));
        assertThat(publicEvent.get(1).payload().getString("subjectId"), is(subjectId.toString()));
        assertThat(publicEvent.get(1).payload().getJsonObject("offenceId"), nullValue());
        assertThat(publicEvent.get(1).payload().getString("hearingId"), is(hearingId.toString()));
        assertThat(publicEvent.get(1).payload().getJsonObject("laaReference"), is(objectToJsonConverter.convert(laaReference)));
        assertThat(publicEvent.get(2).metadata().name(), is("public.progression.defendant-offences-changed"));
    }

    @Test
    public void shouldHandleApplicationOffencesUpdatedEventMessageForMultipleProsecutionCases() {
        final UUID applicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID subjectId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID organisationId = randomUUID();
        final LaaReference laaReference = LaaReference.laaReference().withApplicationReference("applicationReference")
                .withStatusCode("statusCode").withStatusDescription("description").build();

        final List<ApplicationCaseDefendantOrganisation> applicationCaseDefendantOrganisationList = new ArrayList<>();
        final ApplicationCaseDefendantOrganisation applicationCaseDefendantOrganisation = ApplicationCaseDefendantOrganisation.applicationCaseDefendantOrganisation()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withOrganisationId(organisationId)
                .build();

        applicationCaseDefendantOrganisationList.add(applicationCaseDefendantOrganisation);
        final ApplicationReporderOffencesUpdated applicationOffencesUpdated = ApplicationReporderOffencesUpdated.applicationReporderOffencesUpdated()
                .withApplicationId(applicationId)
                .withSubjectId(subjectId)
                .withOffenceId(offenceId)
                .withLaaReference(laaReference)
                .withApplicationCaseDefendantOrganisations(applicationCaseDefendantOrganisationList)
                .build();

        final JsonObject payload = objectToJsonConverter.convert(applicationOffencesUpdated);

        final CourtApplicationEntity applicationEntity = new CourtApplicationEntity();
        applicationEntity.setApplicationId(applicationId);
        applicationEntity.setPayload(payload.toString());

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadataBuilder()
                .withId(randomUUID())
                .withName("progression.event.application-reporder-offences-updated").build());
        when(jsonObjectToObjectConverter.convert(payload, ApplicationReporderOffencesUpdated.class)).thenReturn(applicationOffencesUpdated);
        when(courtApplicationRepository.findByApplicationId(applicationId)).thenReturn(applicationEntity);
        final JsonObject applicationJson = createObjectBuilder().build();
        when(stringToJsonObjectConverter.convert(applicationEntity.getPayload())).thenReturn(applicationJson);
        DefendantCase defendantCase = DefendantCase.defendantCase()
                .withDefendantId(defendantId)
                .withCaseId(caseId)
                .build();
        List<DefendantCase> defendantCases = new ArrayList<>();
        defendantCases.add(defendantCase);
        CourtApplication persistedApplication = CourtApplication.courtApplication()
                .withSubject(CourtApplicationParty.courtApplicationParty()
                        .withId(subjectId)
                        .withMasterDefendant(MasterDefendant.masterDefendant()
                                .withDefendantCase(defendantCases)
                                .build())
                        .build())
                .withCourtApplicationCases(buildCourtApplicationCases(randomUUID(), caseId))
                .build();
        when(jsonObjectToObjectConverter.convert(applicationJson, CourtApplication.class)).thenReturn(persistedApplication);
        HearingSummary hearingSummary = HearingSummary.hearingSummary()
                .withHearingId(hearingId)
                .build();
        List<HearingSummary> hearingSummaryList = new ArrayList<>();
        hearingSummaryList.add(hearingSummary);
        when(progressionService.getHearingsForApplication(applicationId)).thenReturn(Optional.of(hearingSummaryList));

        eventProcessor.handleApplicationOffencesUpdatedEvent(envelope);
        verify(sender, times(2)).send(envelopeCaptor.capture());
        final List<Envelope<JsonObject>> publicEvent = envelopeCaptor.getAllValues();

        assertThat(publicEvent.get(0).metadata().name(), is("public.progression.application-offences-updated"));
        assertThat(publicEvent.get(1).metadata().name(), is("progression.command.update-application-laa-reference-for-hearing"));
    }

    private List<CourtApplicationCase> buildCourtApplicationCases(UUID offenceId, UUID caseId) {
        Offence offence1 = Offence.offence().withId(offenceId).withLaaApplnReference(LaaReference.laaReference().withStatusCode("G2").build()).build();
        Offence offence2 = Offence.offence().withId(randomUUID()).withLaaApplnReference(LaaReference.laaReference().withStatusCode("G2").build()).build();

        CourtApplicationCase courtApplicationCase1 = CourtApplicationCase.courtApplicationCase()
                .withProsecutionCaseId(caseId)
                .withOffences(List.of(offence1, offence2)).build();
        return List.of(courtApplicationCase1);
    }
}