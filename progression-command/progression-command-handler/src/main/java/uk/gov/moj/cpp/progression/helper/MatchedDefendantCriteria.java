package uk.gov.moj.cpp.progression.helper;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.substring;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Person;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObjectBuilder;


public class MatchedDefendantCriteria {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Integer DEFAULT_PAGE_SIZE = 25;
    private static final boolean DEFAULT_PROCEEDINGS_CONCLUDED = false;
    private static final boolean DEFAULT_CROWN_OR_MAGISTRATES = true;

    private static final Integer EXACT = 1;
    private static final Integer PARTIAL = 2;

    private static final String PAGE_SIZE = "pageSize";
    private static final String PROCEEDINGS_CONCLUDED = "proceedingsConcluded";
    private static final String CROWN_OR_MAGISTRATES = "crownOrMagistrates";
    private static final String COURT_ORDER_VALIDITY_DATE = "courtOrderValidityDate";

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

    private Integer exactStep = 1;
    private Integer totalExactSteps = 3;
    private Integer partialStep = 1;
    private Integer totalPartialSteps = 5;
    private Integer subStep = 1;
    private Integer totalSubSteps = 1;
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
                this.totalSubSteps = getSubStepCount(PNC_ID);
                this.partialCriteria = getCriteria(PARTIAL, PNC_ID);
                incrementStep(PARTIAL);
                return true;
            } else {
                incrementMainStep(PARTIAL);
            }
        }
        return false;
    }

    private boolean getSecondPartialStep() {
        if (partialStep == 2) {
            if (isNotEmpty(this.croNumber)) {
                this.partialCriteria = getCriteria(PARTIAL, CRO_NUMBER);
                incrementStep(PARTIAL);
                return true;
            } else {
                incrementMainStep(PARTIAL);
            }
        }
        return false;
    }

    private boolean getThirdPartialStep() {
        if (partialStep == 3) {
            if (isNotEmpty(this.addressLine1)) {
                this.partialCriteria = getCriteria(PARTIAL, DATE_OF_BIRTH, ADDRESS_LINE, LAST_NAME);
                incrementStep(PARTIAL);
                return true;
            } else {
                incrementMainStep(PARTIAL);
            }
        }
        return false;
    }

    private boolean getFourthPartialStep() {
        if (partialStep == 4) {
            if (isNotEmpty(this.dateOfBirth)) {
                this.partialCriteria = getCriteria(PARTIAL, DATE_OF_BIRTH, LAST_NAME);
                incrementStep(PARTIAL);
                return true;
            } else {
                incrementMainStep(PARTIAL);
            }
        }
        return false;
    }

    private boolean getFifthPartialStep() {
        if (partialStep == 5) {
            if (isNotEmpty(this.dateOfBirth) && isNotEmpty(this.addressLine1)) {
                this.partialCriteria = getCriteria(PARTIAL, DATE_OF_BIRTH, ADDRESS_LINE);
                incrementStep(PARTIAL);
                return true;
            } else {
                incrementMainStep(PARTIAL);
            }
        }
        return false;
    }

    private boolean getThirdExactStep() {
        if (exactStep == 3) {
            if (isNotEmpty(this.firstName) && isNotEmpty(this.addressLine1) && isNotEmpty(this.dateOfBirth)) {
                this.exactCriteria = getCriteria(EXACT, FIRST_NAME, DATE_OF_BIRTH, ADDRESS_LINE);
                incrementStep(EXACT);
                return true;
            } else {
                incrementMainStep(EXACT);
            }
        }
        return false;
    }

    private boolean getSecondExactStep() {
        if (exactStep == 2) {
            if (isNotEmpty(this.croNumber)) {
                this.exactCriteria = getCriteria(EXACT, CRO_NUMBER);
                incrementStep(EXACT);
                return true;
            } else {
                incrementMainStep(EXACT);
            }
        }
        return false;
    }

    private boolean getFirstExactStep() {
        if (exactStep == 1) {
            if (isNotEmpty(this.pncId)) {
                this.totalSubSteps = getSubStepCount(PNC_ID);
                this.exactCriteria = getCriteria(EXACT, PNC_ID);
                incrementStep(EXACT);
                return true;
            } else {
                incrementMainStep(EXACT);
            }
        }
        return false;
    }

    private void incrementStep(final int criteriaType) {
        incrementSubStep();
        if (this.subStep > this.totalSubSteps) {
            incrementMainStep(criteriaType);
        }
    }

    private void incrementSubStep() {
        this.subStep++;
    }

    private void incrementMainStep(final int criteriaType) {
        if (EXACT == criteriaType) {
            this.exactStep++;
        } else if (PARTIAL == criteriaType) {
            this.partialStep++;
        }
        resetSubSteps();
    }

    private void resetSubSteps() {
        this.subStep = 1;
        this.totalSubSteps = 1;
    }

    public boolean hasMorePartialSteps() {
        return this.partialStep <= this.totalPartialSteps;
    }

    public boolean hasMoreExactSteps() {
        return this.exactStep <= this.totalExactSteps;
    }

    public boolean hasMoreSubSteps() {
        return this.subStep <= this.totalSubSteps;
    }

    public int getCurrentExactStep() {
        return this.exactStep;
    }

    public int getCurrentPartialStep() {
        return this.partialStep;
    }

    private int getSubStepCount(final String key) {
        if (PNC_ID.equals(key) && isNotEmpty(this.pncId)
                && (isSpiStandardPncId(this.pncId) || isCjsStandardPncId(this.pncId))) {
            return 2;
        }
        return 1;
    }

    private JsonObjectBuilder getCriteria(final int criteriaType, final String... keys) {
        this.criteriaMap.clear();
        if (EXACT == criteriaType) {
            addCriteria(LAST_NAME);
        }
        addCriteria(keys);
        return getDefaultCriteriaBuilder();
    }

    private void addCriteria(final String... keys) {
        for (final String key : keys) {
            addCriteria(key);
        }
    }

    private void addCriteria(final String key) {
        switch (key) {
            case PNC_ID:
                this.criteriaMap.put(PNC_ID, getPncId());
                break;
            case CRO_NUMBER:
                this.criteriaMap.put(CRO_NUMBER, this.croNumber);
                break;
            case LAST_NAME:
                this.criteriaMap.put(LAST_NAME, this.lastName);
                break;
            case FIRST_NAME:
                this.criteriaMap.put(FIRST_NAME, this.firstName);
                break;
            case DATE_OF_BIRTH:
                this.criteriaMap.put(DATE_OF_BIRTH, this.dateOfBirth);
                break;
            case ADDRESS_LINE:
                this.criteriaMap.put(ADDRESS_LINE, this.addressLine1);
                break;
            default:
                break;
        }
    }

    private String getPncId() {
        if (isNotEmpty(this.pncId) && this.totalSubSteps == 2) {
            if (this.subStep == 1 && isSpiStandardPncId(this.pncId)) {
                return convertFromSpiToCjsStandardPncId(this.pncId);
            } else if (this.subStep == 2 && isCjsStandardPncId(this.pncId)) {
                return convertFromCjsToSpiStandardPncId(this.pncId);
            }
        }
        return this.pncId;
    }

    private boolean isSpiStandardPncId(final String pncId) {
        return (pncId.length() == 12 && !pncId.contains("/"));
    }

    private boolean isCjsStandardPncId(final String pncId) {
        return (pncId.length() == 13 && pncId.contains("/"));
    }

    private String convertFromSpiToCjsStandardPncId(final String pncId) {
        return substring(pncId, 0, 4).concat("/").concat(substring(pncId, 4, pncId.length()));
    }

    private String convertFromCjsToSpiStandardPncId(final String pncId) {
        return pncId.replace("/", "");
    }

    private JsonObjectBuilder getDefaultCriteriaBuilder() {
        final JsonObjectBuilder jsonObjectBuilder = JsonObjects.createObjectBuilder();
        jsonObjectBuilder.add(PAGE_SIZE, DEFAULT_PAGE_SIZE)
                .add(PROCEEDINGS_CONCLUDED, DEFAULT_PROCEEDINGS_CONCLUDED)
                .add(COURT_ORDER_VALIDITY_DATE, LocalDate.now().toString())
                .add(CROWN_OR_MAGISTRATES, DEFAULT_CROWN_OR_MAGISTRATES);

        this.criteriaMap.entrySet().stream()
                .forEach(c -> jsonObjectBuilder.add(c.getKey(), c.getValue()));

        return jsonObjectBuilder;
    }

}
