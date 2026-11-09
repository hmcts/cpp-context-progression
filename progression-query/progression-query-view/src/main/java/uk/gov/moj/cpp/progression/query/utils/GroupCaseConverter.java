package uk.gov.moj.cpp.progression.query.utils;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.progression.query.view.service.HearingAtAGlanceService.getDefendantName;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class GroupCaseConverter {

    private static final String CASE_ID = "caseId";
    private static final String CASE_URN = "caseUrn";
    private static final String DEFENDANT_NAME = "defendantName";
    private static final String DEFENDANT_DOB = "defendantDoB";
    private static final String ADDRESS = "address";
    private static final String ADDRESS_1 = "address1";
    private static final String ADDRESS_2 = "address2";
    private static final String ADDRESS_3 = "address3";
    private static final String ADDRESS_4 = "address4";
    private static final String ADDRESS_5 = "address5";
    private static final String POSTCODE = "postcode";

    private static final Comparator<ProsecutionCase> LEAD_CASE_FIRST =
            Comparator.comparing(prosecutionCase -> !Boolean.TRUE.equals(prosecutionCase.getIsGroupMaster()));

    private static final Comparator<ProsecutionCase> BY_DEFENDANT_FIRST_NAME =
            Comparator.comparing(GroupCaseConverter::getDefendantFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    public Optional<JsonObject> toLeadCase(final List<ProsecutionCase> prosecutionCases) {
        return prosecutionCases.stream()
                .filter(prosecutionCase -> Boolean.TRUE.equals(prosecutionCase.getIsGroupMaster()))
                .findFirst()
                .map(this::toCaseSummary);
    }

    public JsonArrayBuilder toCaseSummaries(final List<ProsecutionCase> prosecutionCases) {
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();
        prosecutionCases.stream()
                .sorted(LEAD_CASE_FIRST.thenComparing(BY_DEFENDANT_FIRST_NAME))
                .map(this::toCaseSummary)
                .forEach(jsonArrayBuilder::add);
        return jsonArrayBuilder;
    }

    private static String getDefendantFirstName(final ProsecutionCase prosecutionCase) {
        return prosecutionCase.getDefendants().stream()
                .findFirst()
                .map(defendant -> {
                    if (defendant.getPersonDefendant() != null && defendant.getPersonDefendant().getPersonDetails() != null) {
                        return defendant.getPersonDefendant().getPersonDetails().getFirstName();
                    }
                    if (defendant.getLegalEntityDefendant() != null && defendant.getLegalEntityDefendant().getOrganisation() != null) {
                        return defendant.getLegalEntityDefendant().getOrganisation().getName();
                    }
                    return null;
                })
                .orElse(null);
    }

    private JsonObject toCaseSummary(final ProsecutionCase prosecutionCase) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add(CASE_ID, prosecutionCase.getId().toString());

        final String caseUrn = extractCaseUrn(prosecutionCase);
        if (caseUrn != null) {
            builder.add(CASE_URN, caseUrn);
        }

        prosecutionCase.getDefendants().stream()
                .findFirst()
                .ifPresent(defendant -> addDefendantDetails(builder, defendant));

        return builder.build();
    }

    private void addDefendantDetails(final JsonObjectBuilder builder, final Defendant defendant) {
        builder.add(DEFENDANT_NAME, getDefendantName(defendant.getPersonDefendant(), defendant.getLegalEntityDefendant()));

        if (defendant.getPersonDefendant() != null) {
            final Person personDetails = defendant.getPersonDefendant().getPersonDetails();
            if (personDetails.getDateOfBirth() != null) {
                builder.add(DEFENDANT_DOB, personDetails.getDateOfBirth().toString());
            }
            if (personDetails.getAddress() != null) {
                builder.add(ADDRESS, toAddressJson(personDetails.getAddress()));
            }
        }
    }

    private JsonObjectBuilder toAddressJson(final Address address) {
        final JsonObjectBuilder addressBuilder = createObjectBuilder();
        addIfPresent(addressBuilder, ADDRESS_1, address.getAddress1());
        addIfPresent(addressBuilder, ADDRESS_2, address.getAddress2());
        addIfPresent(addressBuilder, ADDRESS_3, address.getAddress3());
        addIfPresent(addressBuilder, ADDRESS_4, address.getAddress4());
        addIfPresent(addressBuilder, ADDRESS_5, address.getAddress5());
        addIfPresent(addressBuilder, POSTCODE, address.getPostcode());
        return addressBuilder;
    }

    private void addIfPresent(final JsonObjectBuilder builder, final String key, final String value) {
        if (value != null) {
            builder.add(key, value);
        }
    }

    private String extractCaseUrn(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseIdentifier pci = prosecutionCase.getProsecutionCaseIdentifier();
        if (pci == null) {
            return null;
        }
        return pci.getCaseURN() != null ? pci.getCaseURN() : pci.getProsecutionAuthorityReference();
    }
}
