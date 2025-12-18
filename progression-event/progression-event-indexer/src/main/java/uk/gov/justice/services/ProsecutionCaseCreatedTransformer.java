package uk.gov.justice.services;

import static java.util.Objects.nonNull;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;
import uk.gov.justice.services.unifiedsearch.client.domain.Party;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bazaarvoice.jolt.Transform;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProsecutionCaseCreatedTransformer implements Transform {

    private final DomainToIndexMapper domainToIndexMapper = new DomainToIndexMapper();

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Override
    public Object transform(final Object input) {

        final ProsecutionCaseCreated prosecutionCaseCreated =
                new JsonObjectToObjectConverter(objectMapper)
                        .convert(new ObjectToJsonObjectConverter(objectMapper)
                                .convert(input), ProsecutionCaseCreated.class);

        final CaseDetails caseDetails = new CaseDetails();
        final ProsecutionCase prosecutionCase = prosecutionCaseCreated.getProsecutionCase();
        final List<Defendant> defendants = prosecutionCase.getDefendants();
        final List<Party> parties = new ArrayList<>();
        for (final Defendant defendant : defendants) {
            final UUID prosecutionCaseId = defendant.getProsecutionCaseId();
            caseDetails.setCaseId(prosecutionCaseId.toString());
            caseDetails.set_case_type("PROSECUTION");
            if (nonNull(prosecutionCase.getMigrationSourceSystem())
                    && nonNull(prosecutionCase.getMigrationSourceSystem().getMigrationCaseStatus())) {
                caseDetails.setCaseStatus(prosecutionCase.getMigrationSourceSystem().getMigrationCaseStatus().name());
            } else {
                caseDetails.setCaseStatus("ACTIVE");
            }

            if (prosecutionCase.getProsecutionCaseIdentifier() != null) {
                if (nonNull(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN())) {
                    caseDetails.setCaseReference(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN());
                } else if (nonNull(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference())) {
                    caseDetails.setCaseReference(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference());
                }
                final Prosecutor prosecutor = prosecutionCase.getProsecutor();
                if (nonNull(prosecutor)) {
                    caseDetails.setProsecutingAuthority(prosecutor.getProsecutorCode());
                } else {
                    caseDetails.setProsecutingAuthority(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityCode());
                }
            }

            if (nonNull(prosecutionCase.getMigrationSourceSystem())
                    && nonNull(prosecutionCase.getMigrationSourceSystem().getMigrationSourceSystemCaseIdentifier())) {
                caseDetails.setSourceSystemReference(prosecutionCase.getMigrationSourceSystem().getMigrationSourceSystemCaseIdentifier());
            }

            parties.add(domainToIndexMapper.party(defendant));
        }
        caseDetails.setParties(parties);
        return caseDetails;
    }
}