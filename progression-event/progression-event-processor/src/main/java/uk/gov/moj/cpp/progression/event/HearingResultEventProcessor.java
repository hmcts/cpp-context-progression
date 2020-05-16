package uk.gov.moj.cpp.progression.event;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingResultEventProcessor {

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ProgressionService progressionService;

    @Handles("public.hearing.resulted")
    public void handleHearingResultedPublicEvent(final JsonEnvelope event) {
        this.sender.send(this.enveloper.withMetadataFrom(event, "progression.command.hearing-result")
                .apply(event.payloadAsJsonObject()));

        final HearingResulted hearingResulted = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), HearingResulted.class);
        final List<CourtApplication> courtApplications = hearingResulted.getHearing().getCourtApplications();

        if (isNotEmpty(hearingResulted.getHearing().getProsecutionCases())) {
            final List<ProsecutionCase> prosecutionCasesList = hearingResulted.getHearing().getProsecutionCases();

            for (final ProsecutionCase prosecutionCase : prosecutionCasesList) {
                progressionService.updateCase(event, prosecutionCase, courtApplications);
            }
        }

        if (isNotEmpty(courtApplications)) {

            final List<UUID> applicationIdsHaveOutcome = new ArrayList<>();
            final List<UUID> allApplicationIds = new ArrayList<>();

            courtApplications.forEach(courtApplication -> {
                allApplicationIds.add(courtApplication.getId());
                if (courtApplication.getApplicationOutcome() != null) {
                    applicationIdsHaveOutcome.add(courtApplication.getId());
                }
            });
            progressionService.linkApplicationsToHearing(event, hearingResulted.getHearing(), allApplicationIds, HearingListingStatus.HEARING_RESULTED);
            progressionService.updateCourtApplicationStatus(event, applicationIdsHaveOutcome, ApplicationStatus.FINALISED);

        }
    }
}