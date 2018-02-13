package uk.gov.moj.cpp.progression.query.view.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.DefendantDocument;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantDocumentRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.progression.query.view.converter.CaseProgressionDetailToViewConverter;
import uk.gov.moj.cpp.progression.query.view.response.CaseProgressionDetailView;
import uk.gov.moj.cpp.progression.query.view.response.ProsecutingAuthority;
import uk.gov.moj.cpp.progression.query.view.response.SearchCaseByMaterialIdView;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.UUID;

public class CaseProgressionDetailService {

    @Inject
    DefendantDocumentRepository defendantDocumentRepository;
    @Inject
    private CaseProgressionDetailRepository caseProgressionDetailRepo;
    @Inject
    private DefendantRepository defendantRepository;

    @Inject
    private CaseProgressionDetailToViewConverter caseProgressionDetailToViewConverter;

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseProgressionDetailService.class);


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
    public List<CaseProgressionDetail> getCases(final Optional<String> status, final Optional<UUID> caseId) {

        List<CaseProgressionDetail> caseProgressionDetails;
            caseProgressionDetails =
                    caseProgressionDetailRepo.findByStatusAndCaseID(getCaseStatusList(status.orElse(null)),caseId.orElse(null));
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

    @Transactional
    public CaseProgressionDetailView findCaseByCaseUrn(String caseUrn) {
        try {
            CaseProgressionDetail caseProgressionDetail = caseProgressionDetailRepo.findCaseByCaseUrn(caseUrn);
            return caseProgressionDetailToViewConverter.convert(caseProgressionDetail);
        } catch (NoResultException e) {
            LOGGER.debug("No case found with URN='{}'", caseUrn, e);
        }
        return null;
    }

    public SearchCaseByMaterialIdView searchCaseByMaterialId(final String q) {
        SearchCaseByMaterialIdView searchCaseByMaterialIdView = new SearchCaseByMaterialIdView();
        try {
            CaseProgressionDetail caseDetail = caseProgressionDetailRepo.findByMaterialId(UUID.fromString(q));
            if (caseDetail != null) {
                String caseId = caseDetail.getCaseId().toString();
                ProsecutingAuthority prosecutingAuthority = ProsecutingAuthority.CPS;
                searchCaseByMaterialIdView = new SearchCaseByMaterialIdView(caseId, prosecutingAuthority);
            } else {
                searchCaseByMaterialIdView = new SearchCaseByMaterialIdView(null, null);
            }
        } catch (NoResultException e) {
            LOGGER.error("No case found with materialId='{}'", q, e);
        }
        return searchCaseByMaterialIdView;
    }
}
