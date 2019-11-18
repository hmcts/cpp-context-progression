package uk.gov.moj.cpp.progression.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.BoxworkApplicationReferred;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationUpdated;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.ReferBoxworkApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.handler.ReferBoxWorkApplicationHandler;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)

public class ReferBoxWorkApplicationHandlerTest {

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents
            (BoxworkApplicationReferred.class,
                    ProsecutionCaseDefendantListingStatusChanged.class,
                    CourtApplicationUpdated.class);
    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private ReferBoxWorkApplicationHandler referBoxWorkApplicationHandler;

    private ApplicationAggregate aggregate;

    private static ReferBoxworkApplication createReferBoxWorkApplication() {
        final List<CourtApplication> courtApplications = new ArrayList<>();
        courtApplications.add(CourtApplication.courtApplication().withDueDate(LocalDate.now().plusDays(10)).build());
        return ReferBoxworkApplication.referBoxworkApplication()
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withId(UUID.randomUUID())
                        .withCourtApplications(courtApplications)
                        .build())
                .build();
    }

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        aggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        Assert.assertThat(new ReferBoxWorkApplicationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.refer-boxwork-application")
                ));
    }

    @Test
    public void shouldProcessBoxWorkCommand() throws Throwable {

        final ReferBoxworkApplication referBoxWorkApplication = createReferBoxWorkApplication();
        final JsonEnvelope boxWorkCommand = envelopeFrom(metadataWithRandomUUID(
                "progression.command.refer-boxwork-application"), objectToJsonObjectConverter.convert(referBoxWorkApplication));

        referBoxWorkApplicationHandler.handle(boxWorkCommand);

        final ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);

        (Mockito.verify(eventStream, times(1))).append(argumentCaptor.capture());
        final List<Stream> streams = argumentCaptor.getAllValues();
        final List<Envelope> envelopes = ((Stream<Object>) streams.get(0)).map(value -> (Envelope) value).collect(Collectors.toList());


        assertThat(((JsonEnvelope) envelopes.get(0)).payloadAsJsonObject().getJsonObject("courtApplication").getString("dueDate")
                , is(referBoxWorkApplication.getHearingRequest().getCourtApplications().get(0).getDueDate().toString()));
        assertThat(envelopes.get(0).metadata().name(), is("progression.event.court-application-updated"));

        final JsonObject payload = ((JsonEnvelope) envelopes.get(1)).payloadAsJsonObject();
        assertThat(envelopes.get(1).metadata().name(), is("progression.event.boxwork-application-referred"));
        assertThat(payload.getJsonObject("hearingRequest").getString("id"), is(referBoxWorkApplication.getHearingRequest().getId().toString()));
        assertThat(payload.getJsonObject("hearingRequest").getJsonArray("courtApplications").getJsonObject(0).getString("dueDate")
                , is(referBoxWorkApplication.getHearingRequest().getCourtApplications().get(0).getDueDate().toString()));


    }
}