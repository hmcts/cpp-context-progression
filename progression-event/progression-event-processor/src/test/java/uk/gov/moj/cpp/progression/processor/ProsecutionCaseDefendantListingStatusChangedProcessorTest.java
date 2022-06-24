package uk.gov.moj.cpp.progression.processor;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.progression.courts.HearingForApplicationCreated;
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
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Test
    public void shouldProcessProsecutionCaseDefendantListingStatusChanged_whenListHearingRequestsIsNotEmpty() {

        final UUID hearingId = randomUUID();
        final UUID courtApplicationId = randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final String offenceCode = RandomStringUtils.randomAlphanumeric(8);

        final ProsecutionCase prosecutionCase = getProsecutionCase(caseId, defendantId, offenceId, offenceCode,false);

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
