package uk.gov.moj.cpp.progression.processor;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.cpp.progression.test.CoreTestTemplates.CoreTemplateArguments.toMap;
import static uk.gov.moj.cpp.progression.test.CoreTestTemplates.defaultArguments;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
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
import uk.gov.justice.core.courts.AddedOffencesMovedToHearing;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.courts.RelatedHearingUpdatedForAdhocHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.test.CoreTestTemplates;

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
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();


        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(asList( Defendant.defendant()
                                .withId(defendantId).withOffences(asList(Offence.offence().withId(offenceId).build(), Offence.offence().withId(randomUUID()).build()))
                        .build(), Defendant.defendant()
                        .withId(randomUUID()).withOffences(asList(Offence.offence().withId(randomUUID()).build(), Offence.offence().withId(randomUUID()).build()))
                        .build()))
                .build();

        final JsonObject jsonObject = createObjectBuilder().add("prosecutionCase",  objectToJsonObjectConverter.convert(prosecutionCase)).build();
        when(progressionService.getProsecutionCaseDetailById(any(), any(String.class))).thenReturn(Optional.of(jsonObject));

        final AddedOffencesMovedToHearing addedOffencesMovedToHearing = AddedOffencesMovedToHearing.addedOffencesMovedToHearing()
                .withIsHearingInitiateEnriched(true)
                .withHearingId(randomUUID())
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withNewOffences(Collections.singletonList(Offence.offence()
                                                .withId(offenceId)
                                        .build()))
                .build();


        final Envelope<AddedOffencesMovedToHearing> envelope = Envelope.envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUID("progression.event.added-offences-moved-to-hearing"),
                addedOffencesMovedToHearing);

        nextHearingEventProcessor.processAddedOffencesMovedToHearing(envelope);

        verify(sender, times(2)).send(envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getAllValues().size(), is(2));
        List<Envelope<JsonObject>> events = envelopeArgumentCaptor.getAllValues();
        assertThat(events.get(0).metadata().name(), is("progression.command.update-offences-for-hearing"));

        assertThat(events.get(1).metadata().name(), is("public.progression.related-hearing-updated-for-adhoc-hearing"));


        final RelatedHearingUpdatedForAdhocHearing relatedHearingUpdatedForAdhocHearing = (RelatedHearingUpdatedForAdhocHearing) events.get(1).payload();

        assertThat(relatedHearingUpdatedForAdhocHearing.getHearingId(), is(addedOffencesMovedToHearing.getHearingId()));
        assertThat(relatedHearingUpdatedForAdhocHearing.getProsecutionCases().get(0).getDefendants().size(), is(1));
        assertThat(relatedHearingUpdatedForAdhocHearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().size(), is(1));
        assertThat(relatedHearingUpdatedForAdhocHearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(addedOffencesMovedToHearing.getNewOffences().get(0).getId()));


    }
}
