package uk.gov.justice.services.transformer;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.justice.core.courts.LinkType.FIRST_HEARING;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.services.ApplicationMapper;
import uk.gov.justice.services.CaseDetailsMapper;
import uk.gov.justice.services.PartiesMapper;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.unifiedsearch.client.domain.Application;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;
import uk.gov.justice.services.unifiedsearch.client.domain.Hearing;
import uk.gov.justice.services.unifiedsearch.client.domain.Party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bazaarvoice.jolt.Transform;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseCourtApplicationTransformer implements Transform {

    public static final String PROSECUTION = "PROSECUTION";
    public static final String APPLICATION = "APPLICATION";
    public static final String ACTIVE = "ACTIVE";
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseCourtApplicationTransformer.class);

    protected ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private ApplicationMapper applicationMapper = new ApplicationMapper();

    protected PartiesMapper partiesMapper = new PartiesMapper();

    protected CaseDetailsMapper caseDetailsMapper = new CaseDetailsMapper();

    protected HashMap<String, List<CaseDetails>> createCaseDocumentsFromCourtApplication(final CourtApplication courtApplication) {
        final Map<UUID, CaseDetails> caseDocumentsMap = new HashMap<>();
        transformCourtApplicationStatusChange(courtApplication, caseDocumentsMap);

        final List<CaseDetails> caseDetailsList = new ArrayList<>(caseDocumentsMap.values());
        final HashMap<String, List<CaseDetails>> caseDocuments = new HashMap<>();
        caseDocuments.put("caseDocuments", caseDetailsList);
        return caseDocuments;
    }

    protected Map<UUID, CaseDetails> transformCourtApplicationStatusChange(final CourtApplication courtApplication, final Map<UUID, CaseDetails> caseDocumentsMap) {
        final List<CourtApplicationCase> courtApplicationCases = courtApplication.getCourtApplicationCases();
        final LinkType linkType = courtApplication.getType().getLinkType();
        final List<Application> applications = new ArrayList<>();
        applications.add(applicationMapper.transform(courtApplication));
        if (CollectionUtils.isNotEmpty(courtApplicationCases)) {
            if (FIRST_HEARING.equals(linkType)) {

                transformCourtApplication(courtApplication, caseDocumentsMap);

            } else {
                //linked application
                for (final CourtApplicationCase courtApplicationCase : courtApplicationCases) {
                    final UUID caseId = courtApplicationCase.getProsecutionCaseId();
                    caseDocumentsMap.put(caseId, getCaseDetails(applications, caseId, courtApplicationCase.getCaseStatus()));
                }
            }
        } else if (nonNull(courtApplication.getCourtOrder())) {
            //Handle courtOrder
            final CourtOrder courtOrder = courtApplication.getCourtOrder();
            final List<CourtOrderOffence> offences = courtOrder.getCourtOrderOffences();
            for (final CourtOrderOffence courtOrderOffence : offences) {
                final UUID caseId = courtOrderOffence.getProsecutionCaseId();
                caseDocumentsMap.put(caseId, getCaseDetails(applications, caseId, getDefaultCaseStatus()));
            }
        } else {
            //standalone applications
            transformCourtApplication(courtApplication, caseDocumentsMap);
        }
        return caseDocumentsMap;
    }

    private CaseDetails getCaseDetails(final List<Application> applications, final UUID caseId, final String caseStatus) {
        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseId(caseId.toString());
        caseDetails.setApplications(applications);
        caseDetails.setCaseStatus(caseStatus);
        caseDetails.set_case_type(PROSECUTION);
        return caseDetails;
    }

    protected Map<UUID, CaseDetails> transformCourtApplication(final CourtApplication courtApplication, final Map<UUID, CaseDetails> caseDocumentsMap) {
        return transformCourtApplication(courtApplication, null, null, caseDocumentsMap);
    }

    protected Map<UUID, CaseDetails> transformCourtApplication(final CourtApplication courtApplication, final JurisdictionType jurisdictionType, final List<Hearing> hearings, final Map<UUID, CaseDetails> caseDocumentsMap) {
        final CourtApplicationType applicationType = courtApplication.getType();
        final LinkType linkType = applicationType.getLinkType();

        final List<Application> applications = new ArrayList<>();
        applications.add(applicationMapper.transform(courtApplication));

        final List<Party> parties = partiesMapper.transform(courtApplication);

        final List<CourtApplicationCase> courtApplicationCases = courtApplication.getCourtApplicationCases();
        if (CollectionUtils.isNotEmpty(courtApplicationCases) && !FIRST_HEARING.equals(linkType)) {
            //linked application
            for (final CourtApplicationCase courtApplicationCase : courtApplicationCases) {
                final UUID caseId = courtApplicationCase.getProsecutionCaseId();
                newCaseIfNotExists(caseId, jurisdictionType, applications, parties, hearings, caseDocumentsMap, PROSECUTION);
            }

        } else if (nonNull(courtApplication.getCourtOrder())) {
            //Handle courtOrder
            final CourtOrder courtOrder = courtApplication.getCourtOrder();
            final List<CourtOrderOffence> offences = courtOrder.getCourtOrderOffences();
            for (final CourtOrderOffence courtOrderOffence : offences) {
                final UUID caseId = courtOrderOffence.getProsecutionCaseId();
                newCaseIfNotExists(caseId, jurisdictionType, applications, parties, hearings, caseDocumentsMap, PROSECUTION);
            }
        } else if (!applications.isEmpty()) {
            //standalone application
            final Application firstApp = applications.get(0);
            final UUID caseId = fromString(firstApp.getApplicationId());
            newCaseIfNotExists(caseId, jurisdictionType, applications, parties, hearings, caseDocumentsMap, APPLICATION);
        } else {
            LOGGER.error("Unexpected state .... expecting at least linked cases or only one courtApplication");
        }

        return caseDocumentsMap;
    }

    protected void newCaseIfNotExists(final UUID caseId, final JurisdictionType jurisdictionType, final List<Application> applications, final List<Party> parties, final List<Hearing> hearings, final Map<UUID, CaseDetails> caseDocumentsMap, final String caseType) {

        final CaseDetails existingCase = caseDocumentsMap.computeIfAbsent(caseId, key -> caseDetailsMapper.transform(caseType, key, jurisdictionType));

        existingCase.setApplications(addAll(existingCase.getApplications(), applications));
        existingCase.setHearings(addAll(existingCase.getHearings(), hearings));
        existingCase.setParties(addAll(existingCase.getParties(), parties));

        if (getDefaultCaseStatus() != null) {
            existingCase.setCaseStatus(getDefaultCaseStatus());
        }
    }

    protected abstract String getDefaultCaseStatus();

    private <T extends Object> List<T> addAll(List<T> mainList, List<T> additionalList) {
        if (isEmpty(mainList)) {
            return additionalList;
        }

        if (isEmpty(additionalList)) {
            return mainList;
        }

        for (final T item : additionalList) {
            if (!mainList.contains(item)) {
                mainList.add(item);
            }
        }

        return mainList;
    }
}
