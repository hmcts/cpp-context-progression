package uk.gov.moj.cpp.progression.processor;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.core.courts.SeedingHearing.seedingHearing;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV3;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;

import uk.gov.justice.listing.courts.ListNextHearingsV3;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.random.DateGenerator;
import uk.gov.justice.services.test.utils.core.random.ZonedDateTimeGenerator;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseDefendantListingStatusChangedProcessorTest {

    @InjectMocks
    private ProsecutionCaseDefendantListingStatusChangedProcessor eventProcessor;

    @Mock
    private ListingService listingService;

    @Spy
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Captor
    private ArgumentCaptor<ListCourtHearing> listCourtHearingCaptor;

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Captor
    private ArgumentCaptor<ListNextHearingsV3> listNextHearingsCaptor;

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Test
    public void shouldProcessProsecutionCaseDefendantListingStatusChanged_whenListHearingRequestsIsNotEmpty() {

        final UUID hearingId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final String offenceCode = RandomStringUtils.randomAlphanumeric(8);

        final ProsecutionCase prosecutionCase = getProsecutionCase(caseId, defendantId, offenceId, offenceCode, false);

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(singletonList(prosecutionCase))
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                        .withId(courtApplicationId)
                        .build()))
                .build();
        final ListHearingRequest listHearingRequest = populateListHearingRequest(caseId, defendantId, offenceId);

        final ProsecutionCaseDefendantListingStatusChangedV2 hearingForApplicationCreated = ProsecutionCaseDefendantListingStatusChangedV2.prosecutionCaseDefendantListingStatusChangedV2()
                .withHearing(hearing)
                .withHearingListingStatus(HearingListingStatus.SENT_FOR_LISTING)
                .withListHearingRequests(singletonList(listHearingRequest))
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.prosecutionCase-defendant-listing-status-changed"),
                objectToJsonObjectConverter.convert(hearingForApplicationCreated));

        this.eventProcessor.handleProsecutionCaseDefendantListingStatusChangedEvent(event);

        verify(listingService).listCourtHearing(Mockito.eq(event), listCourtHearingCaptor.capture());
        assertThat(listCourtHearingCaptor.getValue().getHearings().get(0).getId(), is(hearingId));
        assertThat(listCourtHearingCaptor.getValue().getHearings().get(0).getProsecutionCases().get(0).getId(), is(caseId));

    }

    @Test
    public void shouldProcessProsecutionCaseDefendantListingStatusChanged_whenListNextHearingsIsNotEmpty() {
        final UUID prosecutionCaseId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();

        final Hearing hearing = getHearing(prosecutionCaseId, courtApplicationId, hearingId, offenceId, seedingHearingId);
        final ListNextHearingsV3 listNextHearings = buildListNextHearings(hearing, seedingHearingId);

        final ProsecutionCaseDefendantListingStatusChangedV3 hearingForApplicationCreated = ProsecutionCaseDefendantListingStatusChangedV3.prosecutionCaseDefendantListingStatusChangedV3()
                .withHearing(hearing)
                .withHearingListingStatus(HearingListingStatus.SENT_FOR_LISTING)
                .withListNextHearings(listNextHearings)
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.prosecutionCase-defendant-listing-status-changed"),
                objectToJsonObjectConverter.convert(hearingForApplicationCreated));

        this.eventProcessor.handleProsecutionCaseDefendantListingStatusChangedEventV3(event);

        verify(listingService).listNextCourtHearings(Mockito.eq(event), listNextHearingsCaptor.capture());
        assertThat(listNextHearingsCaptor.getValue().getHearings().get(0).getId(), is(hearingId));
        assertThat(listNextHearingsCaptor.getValue().getHearings().get(0).getProsecutionCases().get(0).getId(), is(prosecutionCaseId));

    }

    private Hearing getHearing(final UUID prosecutionCaseId, final UUID courtApplicationId, final UUID hearingId,
                               final UUID offenceId, final UUID seedingHearingId) {
        final Hearing hearing = getHearingBuilder(Hearing.hearing()
                .withId(hearingId), getProsecutionCaseCases(prosecutionCaseId, offenceId))
                .withCourtApplications(getCourtApplications(courtApplicationId))
                .withSeedingHearing(seedingHearing().withSeedingHearingId(seedingHearingId).build())
                .build();
        return hearing;
    }

    private List<CourtApplication> getCourtApplications(final UUID courtApplicationId) {
        return asList(
                CourtApplication.courtApplication()
                        .withId(courtApplicationId)
                        .withSubject(CourtApplicationParty.courtApplicationParty().build())
                        .build()
        );
    }

    private Hearing.Builder getHearingBuilder(final Hearing.Builder hearingBuilder, final List<ProsecutionCase> prosecutionCases) {
        return hearingBuilder
                .withProsecutionCases(
                        prosecutionCases
                );
    }

    private List<ProsecutionCase> getProsecutionCaseCases(final UUID prosecutionCaseId, final UUID offenceId) {
        final List<Defendant> defendantList = new ArrayList<>();
        defendantList.add(Defendant.defendant().withId(randomUUID())
                .withOffences(singletonList(Offence.offence()
                        .withId(offenceId)
                        .build()))
                .build());
        return asList(
                ProsecutionCase.prosecutionCase()
                        .withId(prosecutionCaseId)
                        .withDefendants(defendantList)
                        .build()
        );
    }

    private ListNextHearingsV3 buildListNextHearings(final Hearing hearing, final UUID seedingHearingId) {
        return ListNextHearingsV3.listNextHearingsV3()
                .withHearings(Arrays.asList(HearingListingNeeds.hearingListingNeeds()
                        .withId(hearing.getId())
                        .withProsecutionCases(hearing.getProsecutionCases())
                        .build()))
                .withSeedingHearing(seedingHearing().withSeedingHearingId(seedingHearingId).build())
                .build();
    }

    private ListHearingRequest populateListHearingRequest(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        return ListHearingRequest.listHearingRequest()
                .withListDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(caseId)
                        .withDefendantId(defendantId)
                        .withDefendantOffences(Arrays.asList(offenceId))
                        .build()))
                .withListedStartDateTime(new ZonedDateTimeGenerator(Period.ofDays(20), ZonedDateTime.now(), DateGenerator.Direction.FUTURE).next())
                .build();
    }

    private ProsecutionCase getProsecutionCase(final UUID caseId, final UUID defendantId, final UUID offenceId, final String offenceCode, final boolean isYouth) {
        return getProsecutionCase(caseId, defendantId, offenceId, offenceCode, isYouth, ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build());
    }

    private ProsecutionCase getProsecutionCase(final UUID caseId, final UUID defendantId, final UUID offenceId, final String offenceCode, final boolean isYouth, final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        final ProsecutionCase.Builder builder = new ProsecutionCase.Builder().withId(caseId)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier);

        final Defendant.Builder defendantBuilder = new Defendant.Builder()
                .withId(defendantId)
                .withOffences(Stream.of(Offence.offence()
                                .withId(offenceId)
                                .withOffenceCode(offenceCode)
                                .build())
                        .collect(Collectors.toList()));
        if (isYouth) {
            defendantBuilder.withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(Person.person().withDateOfBirth(LocalDate.of(2005, 11, 11)).build()).build());
        }
        builder.withDefendants(Arrays.asList(defendantBuilder.build()));

        return builder.build();
    }
}
