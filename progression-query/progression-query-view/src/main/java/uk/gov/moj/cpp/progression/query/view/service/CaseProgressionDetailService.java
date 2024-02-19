package uk.gov.moj.cpp.progression.query.view.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.DefendantDocument;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantDocumentRepository;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.progression.query.view.response.CaseProgressionDetailView;
import uk.gov.moj.cpp.progression.query.view.response.DefendantDocumentView;
import uk.gov.moj.cpp.progression.query.view.response.ProsecutingAuthority;
import uk.gov.moj.cpp.progression.query.view.response.SearchCaseByMaterialIdView;
/**
 *
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class CaseProgressionDetailService {

    @Inject
    DefendantDocumentRepository defendantDocumentRepository;
    @Inject
    private CaseProgressionDetailRepository caseProgressionDetailRepo;
    @Inject
    private DefendantRepository defendantRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseProgressionDetailService.class);


    @Transactional
    public CaseProgressionDetailView getCaseProgressionDetail(final UUID caseId) {
        CaseProgressionDetail caseProgressionDetail;

        try {
            caseProgressionDetail = caseProgressionDetailRepo.findByCaseId(caseId);
        } catch (final NoResultException nre) {
            LOGGER.error("No CaseProgressionDetail found for caseId: " + caseId, nre);
            return null;
        }

        return CaseProgressionDetailView.createCaseView(caseProgressionDetail);
    }

    @Transactional
    public List<CaseProgressionDetailView> getCases(final Optional<String> status) {

        List<CaseProgressionDetail> caseProgressionDetails;

        if (status.isPresent()) {

            caseProgressionDetails =
                    caseProgressionDetailRepo.findByStatus(getCaseStatusList(status.get()));
        } else {
            caseProgressionDetails = caseProgressionDetailRepo.findOpenStatus();

        }
        return getCaseProgressionDetailViewList(caseProgressionDetails);

    }

    private List<CaseProgressionDetailView> getCaseProgressionDetailViewList(final List<CaseProgressionDetail> caseProgressionDetails) {
        return caseProgressionDetails.stream()
                .map(caseProgressionDetail -> CaseProgressionDetailView.createCaseView(caseProgressionDetail))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<CaseProgressionDetailView> getCases(final Optional<String> status, final Optional<UUID> caseId) {

        List<CaseProgressionDetail> caseProgressionDetails;
            caseProgressionDetails =
                    caseProgressionDetailRepo.findByStatusAndCaseID(getCaseStatusList(status.orElse(null)),caseId.orElse(null));
        return getCaseProgressionDetailViewList(caseProgressionDetails);

    }

    public DefendantDocumentView getDefendantDocument(final Optional<String> caseId, final Optional<String> defendantId) {
        if (caseId.isPresent() && defendantId.isPresent()) {
            final DefendantDocument defendantDocument = defendantDocumentRepository.findLatestDefendantDocument(UUID.fromString(caseId.get()),
                    UUID.fromString(defendantId.get()));
            if (defendantDocument != null) {
                return new DefendantDocumentView(defendantDocument.getFileId(),
                        defendantDocument.getFileName(), defendantDocument.getLastModified());
            }
        }
        return null;
    }

    List<CaseStatusEnum> getCaseStatusList(final String status) {
        final List<CaseStatusEnum> listOfStatus = new ArrayList<>();
        final StringTokenizer st = new StringTokenizer(status, ",");
        while (st.hasMoreTokens()) {
            listOfStatus.add(CaseStatusEnum.getCaseStatusk(st.nextToken()));
        }
        return listOfStatus;
    }

    public SearchCaseByMaterialIdView searchCaseByMaterialId(final String q) {
        SearchCaseByMaterialIdView searchCaseByMaterialIdView = new SearchCaseByMaterialIdView();
        try {
            final CaseProgressionDetail caseDetail = caseProgressionDetailRepo.findByMaterialId(UUID.fromString(q));
            if (caseDetail != null) {
                final String caseId = caseDetail.getCaseId().toString();
                final ProsecutingAuthority prosecutingAuthority = ProsecutingAuthority.CPS;
                searchCaseByMaterialIdView = new SearchCaseByMaterialIdView(caseId, prosecutingAuthority);
            } else {
                searchCaseByMaterialIdView = new SearchCaseByMaterialIdView(null, null);
            }
        } catch (final NoResultException e) {
            LOGGER.info("No case found with materialId='{}'", q, e);
        }
        return searchCaseByMaterialIdView;
    }

}
