package uk.gov.moj.cpp.progression.query.view.service;


import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.progression.persistence.repository.OffenceRepository;
import uk.gov.moj.cpp.progression.query.view.response.OffenceView;
import uk.gov.moj.cpp.progression.query.view.response.OffencesView;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class OffencesService {

    static final String FIELD_CASE_ID = "caseId";

    @Inject
    private CaseProgressionDetailRepository caseRepository;

    @Inject
    private OffenceRepository offenceRepository;


    /**
     * Find the offences for a give envelope containing caseId and defendantId
     *
     * @param caseId
     * @param defendantId
     * @return  offences
     */
    public OffencesView findOffences(final String caseId, final String defendantId) {

        final Defendant defendantDetail = caseRepository
                .findCaseDefendants(UUID.fromString(caseId))
                .stream().filter(defendant -> defendant.getDefendantId().equals(UUID.fromString(defendantId))).findFirst().orElse(null);

        //Defendant details not found hence can't extract the pleas information.
        if (defendantDetail == null) {
            return null;
        }
        final List<OffenceView> offences = offenceRepository.findByDefendantOrderByOrderIndex(defendantDetail).stream()
                .map(OffenceView::new).collect(toList());

        return new OffencesView(offences);
    }
}
