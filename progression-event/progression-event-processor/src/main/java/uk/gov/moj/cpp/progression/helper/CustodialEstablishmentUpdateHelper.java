package uk.gov.moj.cpp.progression.helper;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.*;
import static uk.gov.justice.core.courts.CustodialEstablishment.custodialEstablishment;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CustodialEstablishment;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.Offence;
import uk.gov.moj.cpp.progression.domain.pojo.PrisonCustodySuite;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class CustodialEstablishmentUpdateHelper {

    private static final String PRISON_ORGANISATION_NAME = "prisonOrganisationName";
    private static final List<String> JUDICIAL_RESULT_PROMPTS_LOOKUP = asList(PRISON_ORGANISATION_NAME, "hospitalOrganisationName");
    private static final String COURT_APPLICATION_CASES = "courtApplicationCases";
    private static final String JUDICIAL_RESULTS = "judicialResults";
    private static final String JUDICIAL_RESULT_PROMPTS = "judicialResultPrompts";
    private static final String VALUE = "value";

    public Optional<CustodialEstablishment> getDefendantsResultedWithCustodialEstablishmentUpdate(final Defendant defendant, final List<PrisonCustodySuite> prisonCustodySuites) {
        return nonNull(defendant.getOffences()) && !defendant.getOffences().isEmpty()
                ? getCustodialEstablishmentUpdate(defendant.getOffences(), prisonCustodySuites)
                : Optional.empty();
    }

    public Optional<CustodialEstablishment> getCustodialEstablishmentUpdateFromJudicialResults(final JsonObject application, final List<PrisonCustodySuite> prisonCustodySuites) {

        if (!application.containsKey(COURT_APPLICATION_CASES) && !application.containsKey("courtOrder")) {
            return Optional.empty();
        }

        Optional<JsonObject> judicialResultPromptWithCustody;
        if (application.containsKey(COURT_APPLICATION_CASES)) {
            judicialResultPromptWithCustody = application
                    .getJsonArray(COURT_APPLICATION_CASES).stream().map(JsonObject.class::cast)
                    .filter(jr -> nonNull(jr.getJsonArray("offences")))
                    .flatMap(jr -> jr.getJsonArray("offences").stream().map(JsonObject.class::cast))
                    .filter(jr -> nonNull(jr.getJsonArray(JUDICIAL_RESULTS)))
                    .flatMap(jr -> jr.getJsonArray(JUDICIAL_RESULTS).stream().map(JsonObject.class::cast))
                    .filter(jr -> nonNull(jr.getJsonArray(JUDICIAL_RESULT_PROMPTS)))
                    .flatMap(jr -> jr.getJsonArray(JUDICIAL_RESULT_PROMPTS).stream().map(JsonObject.class::cast))
                    .filter(jrp -> PRISON_ORGANISATION_NAME.equalsIgnoreCase(jrp.getString("promptReference"))
                            && StringUtils.isNotEmpty(jrp.getString(VALUE)))
                    .findFirst();
        } else {
            judicialResultPromptWithCustody = application
                    .getJsonObject("courtOrder")
                    .getJsonArray("courtOrderOffences").stream().map(JsonObject.class::cast)
                    .filter(jr -> nonNull(jr.getJsonObject("offence")))
                    .map(jr -> jr.getJsonObject("offence"))
                    .filter(jr -> nonNull(jr.getJsonArray(JUDICIAL_RESULTS)))
                    .flatMap(jr -> jr.getJsonArray(JUDICIAL_RESULTS).stream().map(JsonObject.class::cast))
                    .filter(jr -> nonNull(jr.getJsonArray(JUDICIAL_RESULT_PROMPTS)))
                    .flatMap(jr -> jr.getJsonArray(JUDICIAL_RESULT_PROMPTS).stream().map(JsonObject.class::cast))
                    .filter(jrp -> PRISON_ORGANISATION_NAME.equalsIgnoreCase(jrp.getString("promptReference"))
                            && StringUtils.isNotEmpty(jrp.getString(VALUE)))
                    .findFirst();
        }

        if (judicialResultPromptWithCustody.isPresent()) {
            final Optional<PrisonCustodySuite> prisonCustody = getPrisonCustody(judicialResultPromptWithCustody.get().getString(VALUE), prisonCustodySuites);

            return prisonCustody.map(prisonCustodySuite -> Optional.of(custodialEstablishment()
                    .withId(prisonCustodySuite.getId())
                    .withCustody(prisonCustodySuite.getType())
                    .withName(prisonCustodySuite.getName())
                    .build()))
                    .orElseGet(() -> Optional.of(custodialEstablishment()
                            .withId(UUID.randomUUID())
                            .withCustody(judicialResultPromptWithCustody.get().toString())
                            .withName(judicialResultPromptWithCustody.get().toString())
                            .build()));
        }


        return Optional.empty();
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
