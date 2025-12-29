package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.CasesAddedForUpdatedRelatedHearing;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.progression.courts.RelatedCaseRequestedForAdhocHearing;
import uk.gov.justice.progression.courts.RelatedHearingRequested;
import uk.gov.justice.progression.courts.RelatedHearingRequestedForAdhocHearing;
import uk.gov.justice.progression.courts.RelatedHearingUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)

public class RelatedHearingEventProcessorTest {

    @InjectMocks
    private RelatedHearingEventProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private ProgressionService progressionService;

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<UUID>> caseIdsArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<ProsecutionCase>> prosecutionCasesArgumentCaptor;

    @Test
    public void shouldIssueUpdateRelatedHearingCommand() {

        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build();
        final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .build();
        final RelatedHearingRequested relatedHearingRequested = RelatedHearingRequested.relatedHearingRequested()
                .withSeedingHearing(seedingHearing)
                .withHearingRequest(hearingListingNeeds)
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.related-hearing-requested"),
                objectToJsonObjectConverter.convert(relatedHearingRequested));

        this.eventProcessor.processRelatedHearingRequested(event);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.update-related-hearing"));
        assertThat(envelopeArgumentCaptor.getValue().payload(), notNullValue());
        assertThat(envelopeArgumentCaptor.getValue().payload().toString(), isJson(allOf(
                withJsonPath("$.hearingRequest", notNullValue()),
                withJsonPath("$.hearingRequest.id", is(hearingId.toString())),
                withJsonPath("$.seedingHearing", notNullValue()),
                withJsonPath("$.seedingHearing.seedingHearingId", is(seedingHearingId.toString())))));

    }

    @Test
    public void shouldIssueUpdateRelatedHearingForAdhocHearingCommand() {

        final UUID hearingId = randomUUID();
        final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .build();
        final RelatedHearingRequestedForAdhocHearing relatedHearingRequestedForAdhocHearing = RelatedHearingRequestedForAdhocHearing.relatedHearingRequestedForAdhocHearing()
                .withHearingRequest(hearingListingNeeds)
                .withSendNotificationToParties(true)
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.related-hearing-requested-for-adhoc-hearing"),
                objectToJsonObjectConverter.convert(relatedHearingRequestedForAdhocHearing));

        this.eventProcessor.processRelatedHearingRequestedForAdhocHearing(event);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.update-related-hearing-for-adhoc-hearing"));
        assertThat(envelopeArgumentCaptor.getValue().payload(), notNullValue());
        assertThat(envelopeArgumentCaptor.getValue().payload().toString(), isJson(allOf(
                withJsonPath("$.sendNotificationToParties", is(true)),
                withJsonPath("$.hearingRequest", notNullValue()),
                withJsonPath("$.hearingRequest.id", is(hearingId.toString())))));

    }

    @Test
    public void shouldIssueUpdateRelatedHearingCommandToListingContextAndHearingContext() {

        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID offenceId = randomUUID();

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build();

        final List<ProsecutionCase> prosecutionCases = Arrays.asList(ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .build());

        final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(prosecutionCases)
                .build();

        final RelatedHearingUpdated relatedHearingUpdated = RelatedHearingUpdated.relatedHearingUpdated()
                .withSeedingHearing(seedingHearing)
                .withHearingRequest(hearingListingNeeds)
                .withShadowListedOffences(Arrays.asList(offenceId))
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.related-hearing-updated"),
                objectToJsonObjectConverter.convert(relatedHearingUpdated));

        this.eventProcessor.processRelatedHearingUpdated(event);

        verify(progressionService).linkProsecutionCasesToHearing(Mockito.eq(event), Mockito.eq(hearingId), caseIdsArgumentCaptor.capture());
        assertThat(caseIdsArgumentCaptor.getValue().get(0), is(prosecutionCaseId));

        verify(sender).send(envelopeArgumentCaptor.capture());

        final Envelope listingCommand = envelopeArgumentCaptor.getValue();

        assertThat(listingCommand.metadata().name(), is("listing.update-related-hearing"));
        assertThat(listingCommand.payload(), notNullValue());
        assertThat(listingCommand.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", is(hearingId.toString())),
                withJsonPath("$.seedingHearing", notNullValue()),
                withJsonPath("$.seedingHearing.seedingHearingId", is(seedingHearingId.toString())),
                withJsonPath("$.prosecutionCases", notNullValue()),
                withJsonPath("$.prosecutionCases[0].id", is(prosecutionCaseId.toString())),
                withJsonPath("$.shadowListedOffences", notNullValue()),
                withJsonPath("$.shadowListedOffences[0]", is(offenceId.toString())))));

        verify(progressionService).updateDefendantYouthForProsecutionCase(Mockito.eq(event), prosecutionCasesArgumentCaptor.capture());

        assertThat(prosecutionCasesArgumentCaptor.getValue(), notNullValue());
        assertThat(prosecutionCasesArgumentCaptor.getValue().get(0).getId(), is(prosecutionCaseId));

    }

    @Test
    public void shouldHandlePublicCasesAddedForUpdatedRelatedHearing() {
        final String hearingId = randomUUID().toString();
        final String seedingHearingId = randomUUID().toString();
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("hearingId", hearingId)
                .add("seedingHearingId", seedingHearingId).build();
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("public.events.listing.cases-added-for-updated-related-hearing"),
                payload
        );
        this.eventProcessor.handlePublicCasesAddedForUpdatedRelatedHearing(event);

        verify(sender).send(envelopeArgumentCaptor.capture());

        final Envelope command = envelopeArgumentCaptor.getValue();
        assertThat(command.metadata().name(), is("progression.command.add-cases-for-updated-related-hearing"));
        assertThat(command.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", is(hearingId)),
                withJsonPath("$.seedingHearingId", is(seedingHearingId)))));

        verify(progressionService).populateHearingToProbationCaseworker(eq(event), eq(fromString(hearingId)));
    }

    @Test
    public void shouldProcessCasesAddedForUpdatedRelatedHearing() {
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID offenceId = randomUUID();

        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withSeedingHearingId(seedingHearingId)
                .build();

        final List<ProsecutionCase> prosecutionCases = Arrays.asList(ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .build());

        final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(prosecutionCases)
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.cases-added-for-updated-related-hearing"),
                objectToJsonObjectConverter.convert(CasesAddedForUpdatedRelatedHearing.casesAddedForUpdatedRelatedHearing()
                        .withSeedingHearing(seedingHearing)
                        .withHearingRequest(hearingListingNeeds)
                        .withShadowListedOffences(Arrays.asList(offenceId))
                        .build()));

        this.eventProcessor.processCasesAddedForUpdatedRelatedHearing(event);

        verify(sender).send(envelopeArgumentCaptor.capture());

        final Envelope hearingCommand = envelopeArgumentCaptor.getValue();
        assertThat(hearingCommand.metadata().name(), is("hearing.update-related-hearing"));
        assertThat(hearingCommand.payload(), notNullValue());
        assertThat(hearingCommand.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", is(hearingId.toString())),
                withJsonPath("$.prosecutionCases", notNullValue()),
                withJsonPath("$.prosecutionCases[0].id", is(prosecutionCaseId.toString())),
                withJsonPath("$.shadowListedOffences", notNullValue()),
                withJsonPath("$.shadowListedOffences[0]", is(offenceId.toString())))));
    }

    @Test
    public void shouldCallCommandWhenRelatedCaseRequestedForAdhocHearing(){
        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.related-case-requested-for-adhoc-hearing"),
                objectToJsonObjectConverter.convert(RelatedCaseRequestedForAdhocHearing.relatedCaseRequestedForAdhocHearing()
                        .withProsecutionCase(ProsecutionCase.prosecutionCase()
                                .withId(prosecutionCaseId)
                                .build())
                        .withListNewHearing(CourtHearingRequest.courtHearingRequest()
                                .withId(hearingId)
                                .build())
                        .withSendNotificationToParties(true)
                        .build()));

        this.eventProcessor.processRelatedCaseRequestedForAdhocHearing(event);

        verify(sender).send(envelopeArgumentCaptor.capture());

        final Envelope hearingCommand = envelopeArgumentCaptor.getValue();
        assertThat(hearingCommand.metadata().name(), is("progression.command.request-related-hearing-for-adhoc-hearing"));
        assertThat(hearingCommand.payload(), notNullValue());
        assertThat(hearingCommand.payload().toString(), isJson(allOf(
                withJsonPath("$.listNewHearing.id", is(hearingId.toString())),
                withJsonPath("$.prosecutionCase", notNullValue()),
                withJsonPath("$.prosecutionCase.id", is(prosecutionCaseId.toString())),
                withJsonPath("$.sendNotificationToParties", is(true))
        )));
    }

}