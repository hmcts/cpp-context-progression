package uk.gov.moj.cpp.progression.query.api;


import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.CaseNotesQueryView;
import uk.gov.moj.cpp.progression.query.DefendantByLAAContractNumberQueryView;
import uk.gov.moj.cpp.progression.query.HearingQueryView;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class ProgressionQueryApi {

    @Inject
    private HearingQueryView hearingQueryView;

    @Inject
    private CaseNotesQueryView caseNotesQueryView;

    @Inject
    private DefendantByLAAContractNumberQueryView defendantByLAAContractNumberQueryView;

    @Handles("progression.query.hearing")
    public JsonEnvelope getHearing(final JsonEnvelope query) {
        return hearingQueryView.getHearing(query);
    }

    @Handles("progression.query.case-notes")
    public JsonEnvelope getCaseNotes(final JsonEnvelope query) {
        return caseNotesQueryView.getCaseNotes(query);
    }
    @Handles("progression.query.defendants-by-laacontractnumber")
    public JsonEnvelope getDefendantsByLAAContractNumber(final JsonEnvelope query) {
        return defendantByLAAContractNumberQueryView.getDefendantsByLAAContractNumber(query);
    }


}
