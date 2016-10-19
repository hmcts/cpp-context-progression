package uk.gov.moj.cpp.progression.query.view.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.progression.persistence.repository.DefendantRepository;

public class CaseProgressionDetailService {

    @Inject
    private CaseProgressionDetailRepository caseProgressionDetailRepo;

    @Inject
    private DefendantRepository defendantRepository;

    @Transactional
    public CaseProgressionDetail getCaseProgressionDetail(final UUID caseId) {
        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findByCaseId(caseId);
        return caseProgressionDetail;
    }


    @Transactional
    public List<CaseProgressionDetail> getCases(final Optional<String> status) {

        List<CaseProgressionDetail> caseProgressionDetails;

        if (status.isPresent()) {

            caseProgressionDetails =
                            caseProgressionDetailRepo.findByStatus(getCaseStatusList(status.get()));
        } else {
            caseProgressionDetails = caseProgressionDetailRepo.findOpenStatus();

        }
        return caseProgressionDetails;

    }


    @Transactional
    public Optional<Defendant> getDefendant(final Optional<String> defendantId) {

        Defendant defendant;

        if (defendantId.isPresent()) {

            defendant = defendantRepository.findByDefendantId(UUID.fromString(defendantId.get()));
            return Optional.of(defendant);
        }
        return Optional.empty();



    }

    @Transactional
    public List<Defendant> getDefendantsByCase(final UUID caseId) {


        final CaseProgressionDetail caseProgressionDetail =
                        caseProgressionDetailRepo.findByCaseId(caseId);
        return new ArrayList(caseProgressionDetail.getDefendants());

    }

    List<CaseStatusEnum> getCaseStatusList(final String status) {
        final List<CaseStatusEnum> listOfStatus = new ArrayList<>();
        final StringTokenizer st = new StringTokenizer(status, ",");
        while (st.hasMoreTokens()) {
            listOfStatus.add(CaseStatusEnum.getCaseStatusk(st.nextToken()));
        }
        return listOfStatus;
    }
}
