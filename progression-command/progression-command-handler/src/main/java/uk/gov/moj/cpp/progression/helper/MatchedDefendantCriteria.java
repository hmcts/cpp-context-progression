package uk.gov.moj.cpp.progression.helper;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Person;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;


public class MatchedDefendantCriteria {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Integer DEFAULT_PAGE_SIZE = 25;
    private static final boolean DEFAULT_PROCEEDINGS_CONCLUDED = false;
    private static final boolean DEFAULT_CROWN_OR_MAGISTRATES = true;

    private static final String PAGE_SIZE = "pageSize";
    private static final String PROCEEDINGS_CONCLUDED = "proceedingsConcluded";
    private static final String CROWN_OR_MAGISTRATES = "crownOrMagistrates";
    private static final String PNC_ID = "pncId";
    private static final String CRO_NUMBER = "croNumber";
    private static final String LAST_NAME = "lastName";
    private static final String FIRST_NAME = "firstName";
    private static final String DATE_OF_BIRTH = "dateOfBirth";
    private static final String ADDRESS_LINE = "addressLine1";

    private final String pncId;
    private final String croNumber;
    private final String lastName;
    private final String firstName;
    private final String dateOfBirth;
    private final String addressLine1;
    private final Map<String, String> criteriaMap;


    private Integer exactStep = 0;
    private Integer partialStep = 0;
    private JsonObjectBuilder exactCriteria;
    private JsonObjectBuilder partialCriteria;


    public MatchedDefendantCriteria(Defendant defendant) {
        final Person personDetails = defendant.getPersonDefendant().getPersonDetails();

        this.pncId = defendant.getPncId();
        this.croNumber = defendant.getCroNumber();
        this.lastName = personDetails.getLastName();
        this.firstName = personDetails.getFirstName();
        this.dateOfBirth = nonNull(personDetails.getDateOfBirth()) ? FORMATTER.format(personDetails.getDateOfBirth()) : "";
        this.addressLine1 = nonNull(personDetails.getAddress()) ? personDetails.getAddress().getAddress1() : "";
        this.criteriaMap = new HashMap<>();
    }

    public boolean nextPartialCriteria() {
        partialStep++;

        if (getFirstPartialStep()) {
            return true;
        }

        if (getSecondPartialStep()) {
            return true;
        }

        if (getThirdPartialStep()) {
            return true;
        }

        if (getFourthPartialStep()) {
            return true;
        }

        return getFifthPartialStep();
    }


    public boolean nextExactCriteria() {
        exactStep++;

        if (getFirstExactStep()) {
            return true;
        }

        if (getSecondExactStep()) {
            return true;
        }

        return getThirdExactStep();
    }

    public JsonObjectBuilder getExactCriteria() {
        return exactCriteria;
    }

    public JsonObjectBuilder getPartialCriteria() {
        return partialCriteria;
    }

    private boolean getFirstPartialStep() {
        if (partialStep == 1) {
            if (isNotEmpty(this.pncId)) {
                this.criteriaMap.keySet().removeAll(criteriaMap.keySet());
                this.criteriaMap.put(PNC_ID, this.pncId);
                this.partialCriteria = getCriteriaForPartialMatch();
                return true;
            } else {
                partialStep++;
            }
        }
        return false;
    }

    private boolean getSecondPartialStep() {
        if (partialStep == 2) {
            if (isNotEmpty(this.croNumber)) {
                this.criteriaMap.keySet().removeAll(criteriaMap.keySet());
                this.criteriaMap.put(CRO_NUMBER, this.croNumber);
                this.partialCriteria = getCriteriaForPartialMatch();
                return true;
            } else {
                partialStep++;
            }
        }
        return false;
    }

    private boolean getThirdPartialStep() {
        if (partialStep == 3) {
            if (isNotEmpty(this.addressLine1)) {
                this.criteriaMap.keySet().removeAll(criteriaMap.keySet());
                this.criteriaMap.put(DATE_OF_BIRTH, this.dateOfBirth);
                this.criteriaMap.put(ADDRESS_LINE, this.addressLine1);
                this.criteriaMap.put(LAST_NAME, this.lastName);
                this.partialCriteria = getCriteriaForPartialMatch();
                return true;
            } else {
                partialStep++;
            }
        }
        return false;
    }

    private boolean getFourthPartialStep() {
        if (partialStep == 4) {
            if (isNotEmpty(this.dateOfBirth)) {
                this.criteriaMap.keySet().removeAll(criteriaMap.keySet());
                this.criteriaMap.put(DATE_OF_BIRTH, this.dateOfBirth);
                this.criteriaMap.put(LAST_NAME, this.lastName);
                this.partialCriteria = getCriteriaForPartialMatch();
                return true;
            } else {
                partialStep++;
            }
        }
        return false;
    }

    private boolean getFifthPartialStep() {
        if (partialStep == 5) {
            if (isNotEmpty(this.dateOfBirth) && isNotEmpty(this.addressLine1)) {
                this.criteriaMap.keySet().removeAll(criteriaMap.keySet());
                this.criteriaMap.put(DATE_OF_BIRTH, this.dateOfBirth);
                this.criteriaMap.put(ADDRESS_LINE, this.addressLine1);
                this.partialCriteria = getCriteriaForPartialMatch();
                return true;
            } else {
                partialStep++;
            }
        }
        return false;
    }

    private boolean getThirdExactStep() {
        if (exactStep == 3) {
            if (isNotEmpty(this.firstName) && isNotEmpty(this.addressLine1) && isNotEmpty(this.dateOfBirth)) {
                this.criteriaMap.keySet().removeAll(criteriaMap.keySet());
                this.criteriaMap.put(FIRST_NAME, this.firstName);
                this.criteriaMap.put(DATE_OF_BIRTH, this.dateOfBirth);
                this.criteriaMap.put(ADDRESS_LINE, this.addressLine1);
                this.exactCriteria = getCriteriaForExactMatch();
                return true;
            } else {
                exactStep++;
            }
        }
        return false;
    }

    private boolean getSecondExactStep() {
        if (exactStep == 2) {
            if (isNotEmpty(this.croNumber)) {
                this.criteriaMap.keySet().removeAll(criteriaMap.keySet());
                this.criteriaMap.put(CRO_NUMBER, this.croNumber);
                this.exactCriteria = getCriteriaForExactMatch();
                return true;
            } else {
                exactStep++;
            }
        }
        return false;
    }

    private boolean getFirstExactStep() {
        if (exactStep == 1) {
            if (isNotEmpty(this.pncId)) {
                this.criteriaMap.keySet().removeAll(criteriaMap.keySet());
                this.criteriaMap.put(PNC_ID, this.pncId);
                this.exactCriteria = getCriteriaForExactMatch();
                return true;
            } else {
                exactStep++;
            }
        }
        return false;
    }


    private JsonObjectBuilder getCriteriaForExactMatch() {
        this.criteriaMap.put(LAST_NAME, this.lastName);

        return getDefaultCriteriaBuilder();
    }

    private JsonObjectBuilder getCriteriaForPartialMatch() {
        return getDefaultCriteriaBuilder();
    }

    private JsonObjectBuilder getDefaultCriteriaBuilder() {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add(PAGE_SIZE, DEFAULT_PAGE_SIZE)
                .add(PROCEEDINGS_CONCLUDED, DEFAULT_PROCEEDINGS_CONCLUDED)
                .add(CROWN_OR_MAGISTRATES, DEFAULT_CROWN_OR_MAGISTRATES);

        this.criteriaMap.entrySet().stream()
                .forEach(c -> jsonObjectBuilder.add(c.getKey(), c.getValue()));

        return jsonObjectBuilder;
    }

}
