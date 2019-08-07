package uk.gov.moj.cpp.progression.event;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationOutcome;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.moj.cpp.progression.service.ProgressionService;

@RunWith(MockitoJUnitRunner.class)
public class HearingResultEventProcessorTest {

    @InjectMocks
    private HearingResultEventProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<UUID>> applicationIdsArgumentCaptor;

    @Captor
    private ArgumentCaptor<Hearing> hearingArgumentCaptor;

    @Captor
    private ArgumentCaptor<ApplicationStatus> applicationStatusArgumentCaptor;

    @Captor
    private ArgumentCaptor<HearingListingStatus> hearingListingStatusArgumentCaptor;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @Mock
    private ProgressionService progressionService;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void handleHearingResultWithoutApplicationOutcome() {

        final UUID courtApplicationId = UUID.randomUUID();
        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                                .withId(courtApplicationId)
                                .build()))
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(hearingResulted));

        this.eventProcessor.handleHearingResultedPublicEvent(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());
        verify(progressionService, atLeastOnce()).linkApplicationsToHearing(envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), hearingListingStatusArgumentCaptor.capture());
        verify(progressionService, atLeastOnce()).updateCourtApplicationStatus(envelopeArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), applicationStatusArgumentCaptor.capture());

        assertThat(hearingArgumentCaptor.getValue(), notNullValue());
        assertThat(hearingArgumentCaptor.getValue().getId(), equalTo(hearingResulted.getHearing().getId()));
        assertThat(applicationIdsArgumentCaptor.getValue(), notNullValue());
        assertEquals(0, applicationIdsArgumentCaptor.getValue().size());
        assertThat(hearingListingStatusArgumentCaptor.getValue(), equalTo(HearingListingStatus.HEARING_RESULTED));
        assertNull(hearingResulted.getHearing().getCourtApplications().get(0).getApplicationOutcome());
    }

    @Test
    public void handleHearingResultWithNoApplications() {

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(hearingResulted));

        this.eventProcessor.handleHearingResultedPublicEvent(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());
        verify(progressionService, never()).linkApplicationsToHearing(envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), hearingListingStatusArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(), jsonEnvelope(
                metadata().withName("progression.command.hearing-result"),
                payloadIsJson(allOf(withJsonPath("$.hearing.id", is(hearingResulted.getHearing().getId().toString()))))));
    }


    @Test
    public void handleHearingResultWithApplicationOutCome() {

        final UUID courtApplicationId = UUID.randomUUID();

        final CourtApplicationOutcome courtApplicationOutcome = CourtApplicationOutcome.courtApplicationOutcome()
                .withApplicationId(courtApplicationId)
                .build();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withHearing(Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                                .withApplicationOutcome(courtApplicationOutcome)
                                .withId(courtApplicationId)
                                .build()))
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.hearing.resulted"),
                objectToJsonObjectConverter.convert(hearingResulted));

        this.eventProcessor.handleHearingResultedPublicEvent(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());
        verify(progressionService, atLeastOnce()).linkApplicationsToHearing(envelopeArgumentCaptor.capture(), hearingArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), hearingListingStatusArgumentCaptor.capture());
        verify(progressionService, atLeastOnce()).updateCourtApplicationStatus(envelopeArgumentCaptor.capture(), applicationIdsArgumentCaptor.capture(), applicationStatusArgumentCaptor.capture());

        assertThat(hearingArgumentCaptor.getValue(), notNullValue());
        assertThat(hearingArgumentCaptor.getValue().getId(), equalTo(hearingResulted.getHearing().getId()));
        assertThat(applicationIdsArgumentCaptor.getValue(), notNullValue());
        assertThat(applicationIdsArgumentCaptor.getValue().get(0), equalTo(courtApplicationId));
        assertThat(hearingListingStatusArgumentCaptor.getValue(), equalTo(HearingListingStatus.HEARING_RESULTED));
        assertThat(applicationStatusArgumentCaptor.getValue(), equalTo(ApplicationStatus.FINALISED));
    }

}