package uk.gov.moj.cpp.progression.handler;



import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import uk.gov.justice.core.courts.CustodialEstablishment;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.UpdateDefendantCustodialInformationForApplication;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

@ExtendWith(MockitoExtension.class)
public class ApplicationCustodialInformationHandlerTest {

    protected static final UUID APPLICATION_ID = UUID.randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private Logger logger;

    @Mock
    protected EventStream eventStream;

    @Mock
    protected ApplicationAggregate applicationAggregate;

    @Mock
    protected Stream<Object> events;

    @Mock
    protected Stream<JsonEnvelope> jsonEvents;

    @Mock
    protected Function function;


    @InjectMocks
    private ApplicationCustodialInformationHandler applicationCustodialInformationHandler;

    @BeforeEach
    public void setup(){
        when(eventSource.getStreamById(APPLICATION_ID)).thenReturn(eventStream);

        when(aggregateService.get(eventStream, ApplicationAggregate.class))
                .thenReturn(applicationAggregate);
    }

    @Test

    public void updateCustodialInformationForApplicationSubject() throws EventStreamException {
        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate()
                .withId(UUID.randomUUID())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withCustody("Prison")
                                .withId(UUID.randomUUID())
                                .withName("HMP/YOI Hewell")
                                .build())

                        .build())

                .build();
        UpdateDefendantCustodialInformationForApplication  updateDefendantCustodialInformationForApplication =UpdateDefendantCustodialInformationForApplication
                .updateDefendantCustodialInformationForApplication()
                .withApplicationId(APPLICATION_ID)
                .withDefendant(defendantUpdate)
                .build();

        when(applicationAggregate.updateCustodialInfomrationForApplicatioNSubject(eq(defendantUpdate), eq(APPLICATION_ID)))
                .thenReturn(events);

        applicationCustodialInformationHandler.handle(Envelope.envelopeFrom(metadataWithRandomUUID("progression.command.update-defendant-custodial-information-for-application"),
                updateDefendantCustodialInformationForApplication));

        verify(applicationAggregate).updateCustodialInfomrationForApplicatioNSubject(defendantUpdate, APPLICATION_ID);
    }


}
