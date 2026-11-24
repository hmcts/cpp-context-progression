package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.List.of;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.events.GroupCreatedFromMovedCases;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupCreatedFromMovedCasesProcessorTest {

    @Spy
    private final JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());
    @Spy
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private CaseDefendantHearingRepository caseDefendantHearingRepository;
    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;
    @Mock
    private Sender sender;

    @InjectMocks
    private GroupCreatedFromMovedCasesProcessor processor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    void shouldPublishPublicConfirmation_AndNothingElse_WhenNoCurrentHearingFound() {
        final UUID groupId = UUID.randomUUID();
        final UUID leadCaseId = UUID.randomUUID();
        final UUID movedCaseId = UUID.randomUUID();

        final JsonEnvelope event = givenGroupCreatedFromMovedCasesEvent(groupId, leadCaseId, of(leadCaseId, movedCaseId));

        when(caseDefendantHearingRepository.findByCaseId(leadCaseId)).thenReturn(of());

        processor.processEvent(event);

        verify(sender, times(1)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue(), jsonEnvelope(
                metadata().withName("public.progression.group-created-from-moved-cases"),
                payloadIsJson(allOf(
                        withJsonPath("$.groupId", is(groupId.toString())),
                        withJsonPath("$.leadCase.id", is(leadCaseId.toString()))
                ))));
    }

    @Test
    void shouldNotMirrorAHearingThatHasAlreadyBeenResulted() {
        final UUID groupId = UUID.randomUUID();
        final UUID leadCaseId = UUID.randomUUID();

        final JsonEnvelope event = givenGroupCreatedFromMovedCasesEvent(groupId, leadCaseId, of(leadCaseId));

        final CaseDefendantHearingEntity resultedHearingLink = caseDefendantHearingEntityFor(resultedHearingEntity());
        when(caseDefendantHearingRepository.findByCaseId(leadCaseId)).thenReturn(of(resultedHearingLink));

        processor.processEvent(event);

        verify(sender, times(1)).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is("public.progression.group-created-from-moved-cases"));
    }

    @Test
    void shouldMirrorCurrentHearing_AndNotifyListingWithAListHearingRequestPerMovedDefendant() {
        final UUID groupId = UUID.randomUUID();
        final UUID leadCaseId = UUID.randomUUID();
        final UUID otherCaseId = UUID.randomUUID();
        final UUID leadDefendantId = UUID.randomUUID();
        final UUID otherDefendantId = UUID.randomUUID();
        final UUID leadOffenceId = UUID.randomUUID();
        final UUID otherOffenceId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final ZonedDateTime sittingDay = ZonedDateTime.now();

        final JsonEnvelope event = givenGroupCreatedFromMovedCasesEvent(groupId, leadCaseId, of(leadCaseId, otherCaseId));

        final Hearing oldHearing = Hearing.hearing()
                .withId(UUID.randomUUID())
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId).build())
                .withType(new HearingType("Trial", UUID.randomUUID(), null))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withEstimatedDuration("P1D")
                .withPriority("STANDARD")
                .withReportingRestrictionReason("reason")
                .withSpecialRequirements(of("interpreter"))
                .withHearingDays(of(HearingDay.hearingDay()
                        .withSittingDay(sittingDay)
                        .withListedDurationMinutes(60)
                        .build()))
                .build();

        final List<CaseDefendantHearingEntity> currentHearingLink = of(caseDefendantHearingEntityFor(hearingEntityFor(oldHearing, HearingListingStatus.SENT_FOR_LISTING)));
        when(caseDefendantHearingRepository.findByCaseId(leadCaseId)).thenReturn(currentHearingLink);

        final ProsecutionCase leadProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(leadCaseId)
                .withDefendants(of(Defendant.defendant()
                        .withId(leadDefendantId)
                        .withOffences(of(Offence.offence().withId(leadOffenceId).build()))
                        .build()))
                .build();
        final ProsecutionCase otherProsecutionCase = ProsecutionCase.prosecutionCase()
                .withId(otherCaseId)
                .withDefendants(of(Defendant.defendant()
                        .withId(otherDefendantId)
                        .withOffences(of(Offence.offence().withId(otherOffenceId).build()))
                        .build()))
                .build();
        final ProsecutionCaseEntity leadCaseEntity = prosecutionCaseEntityFor(leadCaseId, leadProsecutionCase);
        final ProsecutionCaseEntity otherCaseEntity = prosecutionCaseEntityFor(otherCaseId, otherProsecutionCase);
        when(prosecutionCaseRepository.findByCaseId(leadCaseId)).thenReturn(leadCaseEntity);
        when(prosecutionCaseRepository.findByCaseId(otherCaseId)).thenReturn(otherCaseEntity);

        processor.processEvent(event);

        verify(sender, times(4)).send(envelopeCaptor.capture());

        final List<JsonEnvelope> sentEnvelopes = envelopeCaptor.getAllValues();
        assertThat(sentEnvelopes.get(0).metadata().name(), is("public.progression.group-created-from-moved-cases"));

        final JsonEnvelope leadCaseDetachCommand = sentEnvelopes.get(1);
        assertThat(leadCaseDetachCommand, jsonEnvelope(
                metadata().withName("progression.command.remove-hearing-for-prosecution-case"),
                payloadIsJson(allOf(
                        withJsonPath("$.hearingId", is(oldHearing.getId().toString())),
                        withJsonPath("$.prosecutionCaseId", is(leadCaseId.toString()))
                ))));

        final JsonEnvelope otherCaseDetachCommand = sentEnvelopes.get(2);
        assertThat(otherCaseDetachCommand, jsonEnvelope(
                metadata().withName("progression.command.remove-hearing-for-prosecution-case"),
                payloadIsJson(allOf(
                        withJsonPath("$.hearingId", is(oldHearing.getId().toString())),
                        withJsonPath("$.prosecutionCaseId", is(otherCaseId.toString()))
                ))));

        final JsonEnvelope listingCommand = sentEnvelopes.get(3);
        assertThat(listingCommand, jsonEnvelope(
                metadata().withName("progression.command.update-defendant-listing-status"),
                payloadIsJson(allOf(
                        withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")),
                        withJsonPath("$.hearing.prosecutionCases.length()", is(2)),
                        withJsonPath("$.listHearingRequests.length()", is(1)),
                        withJsonPath("$.listHearingRequests[0].courtCentre.id", is(courtCentreId.toString())),
                        withJsonPath("$.listHearingRequests[0].listDefendantRequests.length()", is(2)),
                        withJsonPath("$.listHearingRequests[0].listDefendantRequests[0].prosecutionCaseId", is(leadCaseId.toString())),
                        withJsonPath("$.listHearingRequests[0].listDefendantRequests[0].defendantId", is(leadDefendantId.toString())),
                        withJsonPath("$.listHearingRequests[0].listDefendantRequests[1].prosecutionCaseId", is(otherCaseId.toString())),
                        withJsonPath("$.listHearingRequests[0].listDefendantRequests[1].defendantId", is(otherDefendantId.toString()))
                ))));

        final JsonObject newHearing = listingCommand.payloadAsJsonObject().getJsonObject("hearing");
        assertThat(newHearing.getString("id"), is(not(oldHearing.getId().toString())));
    }

    private JsonEnvelope givenGroupCreatedFromMovedCasesEvent(final UUID groupId, final UUID leadCaseId, final List<UUID> caseIds) {
        final GroupCreatedFromMovedCases groupCreatedFromMovedCases = GroupCreatedFromMovedCases.groupCreatedFromMovedCases()
                .withGroupId(groupId)
                .withCaseIds(caseIds)
                .withLeadCase(ProsecutionCase.prosecutionCase().withId(leadCaseId).build())
                .build();

        return envelopeFrom(
                metadataWithRandomUUID("progression.event.group-created-from-moved-cases"),
                objectToJsonObjectConverter.convert(groupCreatedFromMovedCases));
    }

    private CaseDefendantHearingEntity caseDefendantHearingEntityFor(final HearingEntity hearingEntity) {
        final CaseDefendantHearingEntity entity = new CaseDefendantHearingEntity();
        entity.setId(new CaseDefendantHearingKey());
        entity.setHearing(hearingEntity);
        return entity;
    }

    private HearingEntity hearingEntityFor(final Hearing hearing, final HearingListingStatus listingStatus) {
        final HearingEntity entity = new HearingEntity();
        entity.setHearingId(hearing.getId());
        entity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());
        entity.setListingStatus(listingStatus);
        return entity;
    }

    private HearingEntity resultedHearingEntity() {
        return hearingEntityFor(Hearing.hearing().withId(UUID.randomUUID()).build(), HearingListingStatus.HEARING_RESULTED);
    }

    private ProsecutionCaseEntity prosecutionCaseEntityFor(final UUID caseId, final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity entity = new ProsecutionCaseEntity();
        entity.setCaseId(caseId);
        entity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return entity;
    }
}
