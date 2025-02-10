package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
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
import uk.gov.justice.core.courts.AddedDefendantsMovedToHearing;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.ProgressionService;

@ExtendWith(MockitoExtension.class)
public class NextHearingEventProcessorTest {

    @Mock
    private Sender sender;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Mock
    private ProgressionService progressionService;
    @InjectMocks
    private NextHearingEventProcessor nextHearingEventProcessor;

    @Captor
    ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Test
    public void shouldRaisePublicEvents(){
        final Hearing hearing = Hearing.hearing()
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .build())
                .withCourtCentre(CourtCentre.courtCentre().withId(randomUUID()).build())
                .withJurisdictionType(JurisdictionType.CROWN)
                .build();

        when(progressionService.retrieveHearing(any(), any(UUID.class))).thenReturn(hearing);

        final AddedDefendantsMovedToHearing addedDefendantsMovedToHearing = AddedDefendantsMovedToHearing.addedDefendantsMovedToHearing()
                .withHearingId(randomUUID())
                .withProsecutionCaseId(randomUUID())
                .withDefendants(Collections.singletonList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(Collections.singletonList(Offence.offence()
                                                .withId(randomUUID())
                                        .build()))
                        .build()))
                .build();


        final JsonEnvelope envelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.added-defendants-moved-to-hearing"),
                objectToJsonObjectConverter.convert(addedDefendantsMovedToHearing));
        nextHearingEventProcessor.processAddedDefendantsMovedToHearing(envelope);

        verify(sender, times(4)).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getAllValues().size(), is(4));
        List<Envelope<JsonObject>> events = envelopeArgumentCaptor.getAllValues();
        assertThat(events.get(0).metadata().name(), is("progression.command.update-hearing-with-new-defendant"));

        assertThat(events.get(1).metadata().name(), is("public.progression.defendants-added-to-court-proceedings"));
        assertThat(events.get(2).metadata().name(), is("public.progression.defendants-added-to-case"));
        assertThat(events.get(3).metadata().name(), is("public.progression.defendants-added-to-hearing"));

        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = jsonObjectToObjectConverter.convert(events.get(1).payload(), DefendantsAddedToCourtProceedings.class);

        assertThat(defendantsAddedToCourtProceedings.getDefendants(), is(addedDefendantsMovedToHearing.getDefendants()));
        assertThat(defendantsAddedToCourtProceedings.getListHearingRequests().get(0).getHearingType(), is(hearing.getType()));
        assertThat(defendantsAddedToCourtProceedings.getListHearingRequests().get(0).getCourtCentre(), is(hearing.getCourtCentre()));
        assertThat(defendantsAddedToCourtProceedings.getListHearingRequests().get(0).getJurisdictionType(), is(hearing.getJurisdictionType()));

    }
}
