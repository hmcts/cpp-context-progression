package uk.gov.justice.services;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.CourtApplicationDeletedBdf;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.unifiedsearch.client.domain.Application;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.bazaarvoice.jolt.Transform;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CourtApplicationDeletedBdfTransformer implements Transform {

    public static final String DELETED_APPLICATION_STATUS = "DELETED";

    private static final String PROSECUTION = "PROSECUTION";
    private static final String APPLICATION = "APPLICATION";

    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Override
    public Object transform(final Object input) {

        final JsonObject jsonObject = new ObjectToJsonObjectConverter(objectMapper).convert(input);
        final CourtApplicationDeletedBdf courtApplicationDeletedBdf =
                new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, CourtApplicationDeletedBdf.class);

        final UUID applicationId = courtApplicationDeletedBdf.getApplicationId();
        final List<UUID> caseIds = ofNullable(courtApplicationDeletedBdf.getCaseIds()).orElse(emptyList());

        // an empty caseDocuments array is rejected by JsonDocumentValidator, so a standalone
        // application (no linked cases) is marked as deleted in its own application document
        final List<CaseDetails> caseDetailsList = caseIds.isEmpty()
                ? List.of(deletedApplicationCaseDetails(applicationId, APPLICATION, applicationId))
                : caseIds.stream()
                        .map(caseId -> deletedApplicationCaseDetails(caseId, PROSECUTION, applicationId))
                        .collect(toList());

        final HashMap<String, List<CaseDetails>> caseDocuments = new HashMap<>();
        caseDocuments.put("caseDocuments", caseDetailsList);
        return caseDocuments;
    }

    private CaseDetails deletedApplicationCaseDetails(final UUID caseId, final String caseType, final UUID applicationId) {
        final Application application = new Application();
        application.setApplicationId(applicationId.toString());
        application.setApplicationStatus(DELETED_APPLICATION_STATUS);

        final CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseId(caseId.toString());
        caseDetails.set_case_type(caseType);
        caseDetails.setApplications(List.of(application));
        return caseDetails;
    }
}
