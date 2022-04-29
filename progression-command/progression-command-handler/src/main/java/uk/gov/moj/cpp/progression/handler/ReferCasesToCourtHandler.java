package uk.gov.moj.cpp.progression.handler;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ReferCasesToCourt;
import uk.gov.justice.core.courts.ReferredDefendant;
import uk.gov.justice.core.courts.ReferredPerson;
import uk.gov.justice.core.courts.ReferredProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.CasesReferredToCourtAggregate;
import uk.gov.moj.cpp.progression.service.MatchedDefendantLoadService;
import javax.inject.Inject;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@SuppressWarnings({"squid:S00112","squid:S2629"})
@ServiceComponent(Component.COMMAND_HANDLER)
public class ReferCasesToCourtHandler  {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferCasesToCourtHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private MatchedDefendantLoadService matchedDefendantLoadService;
    
    @Handles("progression.command.refer-cases-to-court")
    public void handle(final Envelope<ReferCasesToCourt> referCasesToCourtEnvelope) throws EventStreamException {
        LOGGER.debug("progression.command.refer-cases-to-court {}", referCasesToCourtEnvelope.payload());

        final ReferCasesToCourt referCasesToCourt = referCasesToCourtEnvelope.payload();
        for(final ReferredProsecutionCase referredProsecutionCase : referCasesToCourt.getCourtReferral().getProsecutionCases()){
            final List<Defendant> defendants = new ArrayList<>();
            for(final ReferredDefendant referredDefendant : referredProsecutionCase.getDefendants()) {
                if(nonNull(referredDefendant.getPersonDefendant())){
                    defendants.add(transformDefendant(referredDefendant));
                }
            }
            final ProsecutionCase prosecutionCase = transformProsecutionCase(referredProsecutionCase, defendants);
            matchedDefendantLoadService.aggregateDefendantsSearchResultForAProsecutionCase(referCasesToCourtEnvelope, prosecutionCase);
        }

        final EventStream eventStream = eventSource.getStreamById(randomUUID());
        final CasesReferredToCourtAggregate casesReferredToCourtAggregate = aggregateService.get(eventStream, CasesReferredToCourtAggregate.class);
        final Stream<Object> events = casesReferredToCourtAggregate.referCasesToCourt(referCasesToCourt.getCourtReferral());
        appendEventsToStream(referCasesToCourtEnvelope, eventStream, events);
    }

    private ProsecutionCase transformProsecutionCase(final ReferredProsecutionCase referredProsecutionCase, final List<Defendant> defendants) {
        return ProsecutionCase.prosecutionCase()
                .withId(referredProsecutionCase.getId())
                .withProsecutionCaseIdentifier(referredProsecutionCase.getProsecutionCaseIdentifier())
                .withDefendants(defendants)
                .build();
    }

    private Defendant transformDefendant(final ReferredDefendant referredDefendant) {
        final ReferredPerson personDetails = referredDefendant.getPersonDefendant().getPersonDetails();
        return Defendant.defendant()
                .withId(referredDefendant.getId())
                .withPncId(referredDefendant.getPersonDefendant().getPncId())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withFirstName(personDetails.getFirstName())
                                .withLastName(personDetails.getLastName())
                                .withMiddleName(personDetails.getMiddleName())
                                .withDateOfBirth(personDetails.getDateOfBirth())
                                .withAddress(personDetails.getAddress())
                                .build())
                        .build())
                .build();
    }

    private void appendEventsToStream(final Envelope<?> envelope, final EventStream eventStream, final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
