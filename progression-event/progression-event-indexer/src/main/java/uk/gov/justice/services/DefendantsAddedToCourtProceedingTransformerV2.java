package uk.gov.justice.services;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedingsV2;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;
import uk.gov.justice.services.unifiedsearch.client.domain.Party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import com.bazaarvoice.jolt.Transform;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefendantsAddedToCourtProceedingTransformerV2 implements Transform {

    public static final String PROSECUTION = "PROSECUTION";
    public static final String ACTIVE = "ACTIVE";
    private DomainToIndexMapper domainToIndexMapper = new DomainToIndexMapper();

    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Override
    public Object transform(final Object input) {

        final JsonObject jsonObject = new ObjectToJsonObjectConverter(objectMapper).convert(input);

        final DefendantsAddedToCourtProceedingsV2 defendantsAddedToCourtProceedings =
                new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, DefendantsAddedToCourtProceedingsV2.class);

        final Map<UUID, CaseDetails> caseDocumentsMap = new HashMap<>();

        final List<Defendant> defendants = defendantsAddedToCourtProceedings.getDefendants();
        for (final Defendant defendant : defendants) {
            final UUID prosecutionCaseId = defendant.getProsecutionCaseId();
            CaseDetails caseDetails = null;
            CaseDetails caseDetailsExisting = caseDocumentsMap.get(prosecutionCaseId);
            if (caseDetailsExisting == null) {
                final List<Party> parties = new ArrayList<>();
                caseDetails = new CaseDetails();
                caseDetails.setCaseId(prosecutionCaseId.toString());
                caseDetails.set_case_type(PROSECUTION);
                caseDetails.setCaseStatus(ACTIVE);
                caseDetails.setParties(parties);
                caseDetailsExisting = caseDetails;
            }
            caseDetailsExisting.getParties().add(domainToIndexMapper.party(defendant));
            caseDocumentsMap.put(prosecutionCaseId, caseDetailsExisting);
        }
        final List<CaseDetails> caseDetailsList = new ArrayList<>(caseDocumentsMap.values());
        final HashMap<String, List<CaseDetails>> caseDocuments = new HashMap<>();
        caseDocuments.put("caseDocuments", caseDetailsList);
        return caseDocuments;
    }
}