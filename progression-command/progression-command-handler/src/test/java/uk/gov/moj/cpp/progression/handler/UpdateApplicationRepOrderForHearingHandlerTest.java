package uk.gov.moj.cpp.progression.handler;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.ApplicationRepOrderUpdatedForHearing;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.core.courts.FundingType;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateApplicationRepOrderForHearingHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            ApplicationRepOrderUpdatedForHearing.class);

    @InjectMocks
    private UpdateApplicationRepOrderForHearingHandler updateApplicationRepOrderForHearingHandler;

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();
    private static final UUID SUBJECT_ID = randomUUID();

    private static final String LAA_CONTRACT_NUMBER = "LAA1234";
    private static final String ORG_NAME = "Test1";
    private static final String INCORPORATION_NUMBER = "LAAINC1";

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateApplicationRepOrderForHearingHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.handler.update-application-rep-order-for-hearing")
                ));
    }

    @Test
    void shouldProcessCommand() throws Exception {
        final HearingAggregate aggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(aggregate);
        Organisation organisation = Organisation.organisation()
                .withName(ORG_NAME)
                .withIncorporationNumber(INCORPORATION_NUMBER)
                .build();
        JsonObject organisationJson = createObjectBuilder()
                .add("name", organisation.getName())
                .add("incorporationNumber", organisation.getIncorporationNumber())
                .build();

        DefenceOrganisation defenceOrganisation = DefenceOrganisation.defenceOrganisation()
                .withLaaContractNumber(LAA_CONTRACT_NUMBER)
                .withOrganisation(organisation)
                .build();
        JsonObject defenceOrganisationJson = createObjectBuilder()
                .add("laaContractNumber", defenceOrganisation.getLaaContractNumber())
                .add("organisation", organisationJson)
                .build();

        final AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                .withAssociationStartDate(LocalDate.now())
                .withIsAssociatedByLAA(true)
                .withFundingType(FundingType.REPRESENTATION_ORDER)
                .withAssociationEndDate(LocalDate.now().plusYears(1))
                .withDefenceOrganisation(defenceOrganisation)
                .build();
        JsonObject associatedDefenceOrganisationJson = createObjectBuilder()
                .add("associationStartDate", associatedDefenceOrganisation.getAssociationStartDate().toString())
                .add("isAssociatedByLAA", associatedDefenceOrganisation.getIsAssociatedByLAA())
                .add("fundingType", associatedDefenceOrganisation.getFundingType().toString())
                .add("associationEndDate", associatedDefenceOrganisation.getAssociationEndDate().toString())
                .add("defenceOrganisation", defenceOrganisationJson)
                .build();
        JsonObject message = createObjectBuilder()
                .add("hearingId", HEARING_ID.toString())
                .add("applicationId", APPLICATION_ID.toString())
                .add("subjectId", SUBJECT_ID.toString())
                .add("associatedDefenceOrganisation", associatedDefenceOrganisationJson)
                .build();

        when(jsonObjectToObjectConverter.convert(associatedDefenceOrganisationJson, AssociatedDefenceOrganisation.class)).thenReturn(associatedDefenceOrganisation);

        final Hearing hearing = Hearing.hearing()
                .withId(HEARING_ID)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(singletonList(courtApplication()
                        .withId(APPLICATION_ID)
                        .withSubject(CourtApplicationParty.courtApplicationParty().withId(SUBJECT_ID).build())
                        .build()))
                .build();

        aggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.handler.update-application-rep-order-for-hearing")
                .withId(randomUUID())
                .build();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, message);

        updateApplicationRepOrderForHearingHandler.handle(envelope);

        JsonObject event = getEventAsJsonObjectFromStreamInGivenTimes(1, "progression.event.application-rep-order-updated-for-hearing", true);
        assertThat(event, notNullValue());
        assertThat(event.getString("applicationId"), is(APPLICATION_ID.toString()));
        assertThat(event.getString("subjectId"), is(SUBJECT_ID.toString()));
        assertThat(event.getString("hearingId"), is(HEARING_ID.toString()));

        JsonObject associatedDefenceOrganisationObject = event.getJsonObject("associatedDefenceOrganisation");
        assertThat(associatedDefenceOrganisationObject.getString("fundingType"), is("REPRESENTATION_ORDER"));
        assertThat(associatedDefenceOrganisationObject.getBoolean("isAssociatedByLAA"), is(Boolean.TRUE));

        JsonObject defenceOrganisationObject = associatedDefenceOrganisationObject.getJsonObject("defenceOrganisation");
        assertThat(defenceOrganisationObject.getString("laaContractNumber"), is(LAA_CONTRACT_NUMBER));

        JsonObject organisationObject = defenceOrganisationObject.getJsonObject("organisation");
        assertThat(organisationObject.getString("incorporationNumber"), is(INCORPORATION_NUMBER));
        assertThat(organisationObject.getString("name"), is(ORG_NAME));
    }

    private JsonObject getEventAsJsonObjectFromStreamInGivenTimes(int times, String eventName, boolean isEventPresent) throws EventStreamException {
        final ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);
        (Mockito.verify(eventStream, times(times))).append(argumentCaptor.capture());
        final List<Stream> streams2 = argumentCaptor.getAllValues();
        final List<JsonEnvelope> eventsList = (List<JsonEnvelope>) (streams2.get(times-1).collect(Collectors.toList()));
        Optional<JsonEnvelope> eventEnvelope = eventsList.stream().filter(x -> x.metadata().name().equalsIgnoreCase(eventName)).findFirst();
        assertThat(eventEnvelope.isPresent(), is(isEventPresent));
        if (isEventPresent) {
            return eventEnvelope.get().payloadAsJsonObject();
        }
        return null;
    }
}