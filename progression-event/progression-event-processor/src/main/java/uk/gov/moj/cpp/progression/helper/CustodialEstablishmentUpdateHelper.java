package uk.gov.moj.cpp.progression.helper;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static uk.gov.justice.core.courts.CustodialEstablishment.custodialEstablishment;

import uk.gov.justice.core.courts.CustodialEstablishment;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.Offence;
import uk.gov.moj.cpp.progression.domain.pojo.PrisonCustodySuite;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

public class CustodialEstablishmentUpdateHelper {

    private static final List<String> JUDICIAL_RESULT_PROMPTS_LOOKUP = asList("prisonOrganisationName", "hospitalOrganisationName");

    public Optional<CustodialEstablishment> getDefendantsResultedWithCustodialEstablishmentUpdate(final Defendant defendant, final List<PrisonCustodySuite> prisonCustodySuites) {
        return nonNull(defendant.getOffences()) && !defendant.getOffences().isEmpty()
                ? getCustodialEstablishmentUpdate(defendant.getOffences(), prisonCustodySuites)
                : Optional.empty();
    }

    private Optional<CustodialEstablishment> getCustodialEstablishmentUpdate(final List<Offence> offences, final List<PrisonCustodySuite> prisonCustodySuites) {

        final Optional<JudicialResultPrompt> judicialResultPromptWithCustody = offences.stream()
                .filter(offence -> nonNull(offence.getJudicialResults()))
                .flatMap(offence -> offence.getJudicialResults().stream())
                .filter(jr -> nonNull(jr.getJudicialResultPrompts()))
                .flatMap(jr -> jr.getJudicialResultPrompts().stream())
                .filter(jrp -> JUDICIAL_RESULT_PROMPTS_LOOKUP.contains(jrp.getPromptReference())
                        && StringUtils.isNotEmpty(jrp.getValue()))
                .findFirst();

        if (judicialResultPromptWithCustody.isPresent()) {
            final Optional<PrisonCustodySuite> prisonCustody = getPrisonCustody(judicialResultPromptWithCustody.get().getValue(), prisonCustodySuites);

            return prisonCustody.map(prisonCustodySuite -> Optional.of(custodialEstablishment()
                            .withId(prisonCustodySuite.getId())
                            .withCustody(prisonCustodySuite.getType())
                            .withName(prisonCustodySuite.getName())
                            .build()))
                    .orElseGet(() -> Optional.of(custodialEstablishment()
                            .withId(UUID.randomUUID())
                            .withCustody(judicialResultPromptWithCustody.get().getValue())
                            .withName(judicialResultPromptWithCustody.get().getValue())
                            .build()));
        }


        return Optional.empty();
    }

    private Optional<PrisonCustodySuite> getPrisonCustody(final String custodyPromptValue, final List<PrisonCustodySuite> prisonCustodySuites) {
        return prisonCustodySuites.stream().filter(pcs -> pcs.getName().equals(custodyPromptValue)).findFirst();
    }
}
