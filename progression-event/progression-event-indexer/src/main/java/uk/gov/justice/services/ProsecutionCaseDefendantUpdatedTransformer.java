package uk.gov.justice.services;

import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;
import uk.gov.justice.services.unifiedsearch.client.domain.Party;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.bazaarvoice.jolt.Transform;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProsecutionCaseDefendantUpdatedTransformer implements Transform {

    private DomainToIndexMapper domainToIndexMapper = new DomainToIndexMapper();

    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Override
    public Object transform(final Object input) {

        final JsonObject jsonObject = new ObjectToJsonObjectConverter(objectMapper).convert(input);
        final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated =
                new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, ProsecutionCaseDefendantUpdated.class);
        final DefendantUpdate defendant = prosecutionCaseDefendantUpdated.getDefendant();
        final UUID prosecutionCaseId = defendant.getProsecutionCaseId();
        final CaseDetails caseDetails = new CaseDetails();
        final List<Party> parties = new ArrayList<>();
        caseDetails.setCaseId(prosecutionCaseId.toString());
        caseDetails.set_case_type("PROSECUTION");
        parties.add(domainToIndexMapper.party(defendant));
        caseDetails.setParties(parties);
        return caseDetails;
    }
}