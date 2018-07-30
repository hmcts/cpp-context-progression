package uk.gov.moj.cpp.progression.activiti.workflow.listhearing.enricher;

import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.PLEA;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.SEND_CASE_FOR_LISTING_PAYLOAD;
import static uk.gov.moj.cpp.progression.activiti.workflow.listhearing.enricher.HearingType.PTP;
import static uk.gov.moj.cpp.progression.activiti.workflow.listhearing.enricher.HearingType.SENTENCE;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.external.domain.listing.Hearing;
import uk.gov.moj.cpp.external.domain.listing.ListingCase;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

@ServiceComponent(Component.EVENT_PROCESSOR)
@Named
public class ListingHearingEnricher implements JavaDelegate {

    private static final String GUILTY = "GUILTY";

    @Handles("hearing.enricher-dummy")
    public void doesNothing(final JsonEnvelope jsonEnvelope) {
        // required by framework
    }

    @Override
    public void execute(final DelegateExecution delegateExecution) throws Exception {
        final ListingCase savedListingCase = (ListingCase) delegateExecution.getVariable(SEND_CASE_FOR_LISTING_PAYLOAD);
        final List<String> pleas = (List<String>) delegateExecution.getVariable(PLEA);
        final List<Hearing> hearings;
        if (pleas.contains(GUILTY)) {
            hearings = savedListingCase.getHearings().stream().map(h ->
                    new Hearing(h.getId(), h.getCourtCentreId(), SENTENCE.getName(), h.getStartDate(), SENTENCE.getEstimateMinutes(), h.getDefendants(),h.getCourtRoomId(),h.getJudgeId(),h.getStartTime()))
                    .collect(Collectors.toList());
        } else {
            hearings = savedListingCase.getHearings().stream().map(h ->
                    new Hearing(h.getId(), h.getCourtCentreId(), PTP.getName(), h.getStartDate(), PTP.getEstimateMinutes(), h.getDefendants(),h.getCourtRoomId(),h.getJudgeId(),h.getStartTime()))
                    .collect(Collectors.toList());
        }
        final ListingCase listingCaseWithHearingType = new ListingCase(savedListingCase.getCaseId(), savedListingCase.getUrn(), hearings);
        delegateExecution.setVariable(SEND_CASE_FOR_LISTING_PAYLOAD, listingCaseWithHearingType);
    }
}
