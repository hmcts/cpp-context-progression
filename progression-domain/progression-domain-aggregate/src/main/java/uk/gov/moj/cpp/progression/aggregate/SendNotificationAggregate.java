package uk.gov.moj.cpp.progression.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.SendNotificationForApplication;
import uk.gov.justice.core.courts.SendNotificationForApplicationIgnored;
import uk.gov.justice.core.courts.SendNotificationForApplicationInitiated;
import uk.gov.justice.domain.aggregate.Aggregate;

import java.util.stream.Stream;

@SuppressWarnings({"squid:S1068","squid:S1450"})
public class SendNotificationAggregate implements Aggregate {



    private  SendNotificationForApplicationInitiated sendNotificationForApplicationInitiated;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(SendNotificationForApplicationInitiated.class).apply(this::handleSendNotificationInitiated),
                otherwiseDoNothing());
    }

    private void handleSendNotificationInitiated(final SendNotificationForApplicationInitiated sendNotificationForApplicationInitiated) {
        this.sendNotificationForApplicationInitiated = SendNotificationForApplicationInitiated.sendNotificationForApplicationInitiated()
                .withCourtHearing(sendNotificationForApplicationInitiated.getCourtHearing())
                .withBoxHearing(sendNotificationForApplicationInitiated.getBoxHearing())
                .withIsBoxWorkRequest(sendNotificationForApplicationInitiated.getIsBoxWorkRequest())
                .withIsWelshTranslationRequired(sendNotificationForApplicationInitiated.getIsWelshTranslationRequired())
                .withSummonsApprovalRequired(sendNotificationForApplicationInitiated.getSummonsApprovalRequired())
                .build();
    }



    public Stream<Object> sendNotificationForApplication(final SendNotificationForApplication sendNotificationForApplicationInitiated, final boolean applicationReferredToNewHearing, final boolean isWelshTranslationRequired) {
        return apply(
                Stream.of(SendNotificationForApplicationInitiated.sendNotificationForApplicationInitiated()
                        .withCourtApplication(sendNotificationForApplicationInitiated.getCourtApplication())
                        .withCourtHearing(sendNotificationForApplicationInitiated.getCourtHearing())
                        .withBoxHearing(sendNotificationForApplicationInitiated.getBoxHearing())
                        .withSummonsApprovalRequired(sendNotificationForApplicationInitiated.getSummonsApprovalRequired())
                        .withIsBoxWorkRequest(sendNotificationForApplicationInitiated.getIsBoxWorkRequest())
                        .withApplicationReferredToNewHearing(applicationReferredToNewHearing)
                        .withIsWelshTranslationRequired(isWelshTranslationRequired)
                        .build()));
    }

    public Stream<Object> ignoreSendNotificationForApplication(final SendNotificationForApplication sendNotificationForApplication) {
        return apply(
                Stream.of(SendNotificationForApplicationIgnored.sendNotificationForApplicationIgnored()
                        .withCourtApplication(sendNotificationForApplication.getCourtApplication())
                        .withCourtHearing(sendNotificationForApplication.getCourtHearing())
                        .withBoxHearing(sendNotificationForApplication.getBoxHearing())
                        .withIsWelshTranslationRequired(sendNotificationForApplication.getIsWelshTranslationRequired())
                        .build()));

    }
}
