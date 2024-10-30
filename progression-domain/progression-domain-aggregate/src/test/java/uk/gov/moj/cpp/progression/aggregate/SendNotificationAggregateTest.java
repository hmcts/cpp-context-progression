package uk.gov.moj.cpp.progression.aggregate;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.SendNotificationForApplication;
import uk.gov.justice.core.courts.SendNotificationForApplicationIgnored;
import uk.gov.justice.core.courts.SendNotificationForApplicationInitiated;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class SendNotificationAggregateTest {
    @InjectMocks
    private SendNotificationAggregate aggregate;

    @BeforeEach
    public void setUp() {
        aggregate = new SendNotificationAggregate();
    }

    @Test
    public void shouldSaveAccountNumber() {
        final UUID materialId = randomUUID();
        final UUID requestId = randomUUID();
        final SendNotificationForApplication sendNotificationForApplication = SendNotificationForApplication.sendNotificationForApplication().withCourtApplication(
                CourtApplication.courtApplication()
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant().withIsYouth(true).build())
                                .build())
                        .build())
                .withIsWelshTranslationRequired(false)
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .build();
        aggregate.apply(sendNotificationForApplication);
        final Stream<Object> eventStream = aggregate.sendNotificationForApplication(sendNotificationForApplication, false, sendNotificationForApplication.getIsWelshTranslationRequired());
        List<Object> lEvents = eventStream.collect(Collectors.toList());
        assertThat(lEvents.size(), is(1));
        final Object object = lEvents.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(SendNotificationForApplicationInitiated.class)));
        assertThat(lEvents.get(0).getClass(), is(CoreMatchers.equalTo(SendNotificationForApplicationInitiated.class)));
    }

    @Test
    public void shouldIgnoreSendNotificationForApplication() {
        final SendNotificationForApplication sendNotificationForApplication = SendNotificationForApplication.sendNotificationForApplication().withCourtApplication(
                CourtApplication.courtApplication()
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withMasterDefendant(MasterDefendant.masterDefendant().withIsYouth(true).build())
                                .build())
                        .build())
                .withIsWelshTranslationRequired(false)
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .build();
        final Stream<Object> eventStream = aggregate.ignoreSendNotificationForApplication(sendNotificationForApplication);
        List<Object> lEvents = eventStream.collect(Collectors.toList());
        assertThat(lEvents.size(), is(1));
        final Object object = lEvents.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(SendNotificationForApplicationIgnored.class)));
        assertThat(lEvents.get(0).getClass(), is(CoreMatchers.equalTo(SendNotificationForApplicationIgnored.class)));
    }
}
