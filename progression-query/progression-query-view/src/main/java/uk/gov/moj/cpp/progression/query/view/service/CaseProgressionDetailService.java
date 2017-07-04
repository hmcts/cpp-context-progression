package uk.gov.moj.cpp.progression.query.view.service;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.DefendantDocument;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantDocumentRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

public class CaseProgressionDetailService {

    @Inject
    DefendantDocumentRepository defendantDocumentRepository;
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

    public Optional<DefendantDocument> getDefendantDocument(final Optional<String> caseId, final Optional<String> defendantId) {
        if (caseId.isPresent() && defendantId.isPresent()) {
            return Optional.of(defendantDocumentRepository.findLatestDefendantDocument(UUID.fromString(caseId.get()),
                    UUID.fromString(defendantId.get())));
        } else {
            return Optional.empty();
        }
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
