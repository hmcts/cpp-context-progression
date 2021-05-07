package uk.gov.justice.services;

import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingConfirmedCaseStatusUpdated;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;
import uk.gov.justice.services.unifiedsearch.client.domain.Party;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.json.JsonObject;

import com.bazaarvoice.jolt.Transform;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HearingConfirmedCaseStatusUpdatedTransformer implements Transform {

    public static final String PROSECUTION = "PROSECUTION";
    private DomainToIndexMapper domainToIndexMapper = new DomainToIndexMapper();

    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Override
    public Object transform(final Object input) {

        final JsonObject jsonObject = new ObjectToJsonObjectConverter(objectMapper).convert(input);

        final HearingConfirmedCaseStatusUpdated prosecutionCaseCreated =
                new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, HearingConfirmedCaseStatusUpdated.class);
        final CaseDetails caseDetails = new CaseDetails();
        final ProsecutionCase prosecutionCase = prosecutionCaseCreated.getProsecutionCase();

        caseDetails.setCaseId(prosecutionCase.getId().toString());
        caseDetails.set_case_type(PROSECUTION);
        caseDetails.setCaseStatus(prosecutionCaseCreated.getCaseStatus());
        caseDetails.set_is_crown(false);

        if (prosecutionCase.getProsecutionCaseIdentifier() != null) {
            if (!Objects.isNull(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN())) {
                caseDetails.setCaseReference(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN());
            } else if (!Objects.isNull(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference())) {
                caseDetails.setCaseReference(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference());
            }
            final Prosecutor prosecutor = prosecutionCase.getProsecutor();
            if (nonNull(prosecutor)) {
                caseDetails.setProsecutingAuthority(prosecutor.getProsecutorCode());
            } else {
                caseDetails.setProsecutingAuthority(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode());
            }
        }

        final List<Defendant> defendants = prosecutionCase.getDefendants();
        final List<Party> parties = new ArrayList<>();
        for (final Defendant defendant : defendants) {
            parties.add(domainToIndexMapper.party(defendant));
        }
        caseDetails.setParties(parties);
        return caseDetails;
    }
}