package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.moj.cpp.progression.plea.json.schemas.DisabilityNeeds;
import uk.gov.moj.cpp.progression.plea.json.schemas.Frequency;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "online_plea")
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class OnlinePlea {
    @Id
    @Column(name = "case_id", updatable = false, nullable = false)
    private UUID caseId;

    @Column(name = "defendant_id", updatable = false, nullable = false)
    private UUID defendantId;

    @Column(name = "urn", updatable = false, nullable = false)
    private String urn;

    @Column(name = "submitted_on", updatable = false, nullable = false)
    private ZonedDateTime submittedOn;

    @Embedded
    private OnlinePleaPersonalDetails personalDetails;

    @Embedded
    private OnlinePleaLegalEntityDetails legalEntityDetails;

    @Embedded
    private PleaDetails pleaDetails;

    @Embedded
    private Employment employment;

    @Embedded
    private Employer employer;

    @Embedded
    private Outgoings outgoings;

    @Embedded
    private LegalEntityFinancialMeans legalEntityFinancialMeans;

    public OnlinePlea() {
    }


    private OnlinePlea(final UUID caseId, final UUID defendantId, final ZonedDateTime submittedOn) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.submittedOn = submittedOn;
    }

    public OnlinePlea(final Defendant defendantDetail, final DisabilityNeeds disabilityNeeds, final ZonedDateTime updatedDate, final uk.gov.moj.cpp.progression.plea.json.schemas.LegalEntityFinancialMeans legalEntityFinancialMeans) {
        this(defendantDetail.getProsecutionCaseId(), defendantDetail.getId(), updatedDate);
        this.personalDetails = new OnlinePleaPersonalDetails(defendantDetail);
        this.legalEntityDetails = new OnlinePleaLegalEntityDetails(defendantDetail);
        this.legalEntityFinancialMeans = buildLegalEntityFinancialMeans(legalEntityFinancialMeans);
        this.pleaDetails = ofNullable(disabilityNeeds).map(dsNeeds -> {
            if (toBoolean(dsNeeds.getNeeded())) {
                return new PleaDetails(dsNeeds);
            } else {
                return null;
            }
        }).orElse(null);
    }

    private LegalEntityFinancialMeans buildLegalEntityFinancialMeans(final uk.gov.moj.cpp.progression.plea.json.schemas.LegalEntityFinancialMeans legalEntityFinancialMeans) {
        return ofNullable(legalEntityFinancialMeans)
                .map(financialMeans -> new LegalEntityFinancialMeans(financialMeans.getOutstandingFines(), financialMeans.getTradingMoreThan12Months(), financialMeans.getNumberOfEmployees(), financialMeans.getGrossTurnover(), financialMeans.getNetTurnover()))
                .orElse(new LegalEntityFinancialMeans());
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(final String urn) {
        this.urn = urn;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public ZonedDateTime getSubmittedOn() {
        return submittedOn;
    }

    public void setSubmittedOn(final ZonedDateTime submittedOn) {
        this.submittedOn = submittedOn;
    }

    public OnlinePleaPersonalDetails getPersonalDetails() {
        return personalDetails;
    }

    public void setPersonalDetails(final OnlinePleaPersonalDetails personalDetails) {
        this.personalDetails = personalDetails;
    }

    public OnlinePleaLegalEntityDetails getLegalEntityDetails() {
        return legalEntityDetails;
    }

    public void setLegalEntityDetails(final OnlinePleaLegalEntityDetails legalEntityDetails) {
        this.legalEntityDetails = legalEntityDetails;
    }

    public PleaDetails getPleaDetails() {
        return pleaDetails;
    }

    public void setPleaDetails(final PleaDetails pleaDetails) {
        this.pleaDetails = pleaDetails;
    }

    public Employment getEmployment() {
        return employment;
    }

    public void setEmployment(final Employment employment) {
        this.employment = employment;
    }

    public Employer getEmployer() {
        return employer;
    }

    public void setEmployer(final Employer employer) {
        this.employer = employer;
    }

    public Outgoings getOutgoings() {
        return outgoings;
    }

    public void setOutgoings(final Outgoings outgoings) {
        this.outgoings = outgoings;
    }

    public LegalEntityFinancialMeans getLegalEntityFinancialMeans() {
        return legalEntityFinancialMeans;
    }

    public void setLegalEntityFinancialMeans(final LegalEntityFinancialMeans legalEntityFinancialMeans) {
        this.legalEntityFinancialMeans = legalEntityFinancialMeans;
    }


    @Embeddable
    @SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
    public static class PleaDetails {

        @Column(name = "come_to_court")
        private Boolean comeToCourt;
        @Column(name = "interpreter_language")
        private String interpreterLanguage;
        @Column(name = "witness_dispute")
        private String witnessDispute;
        @Column(name = "witness_details")
        private String witnessDetails;
        @Column(name = "unavailability")
        private String unavailability;
        @Column(name = "speak_welsh")
        private Boolean speakWelsh;
        @Column(name = "outstanding_fines")
        private Boolean outstandingFines;
        @Column(name = "disability_needs")
        private String disabilityNeeds;

        public PleaDetails() {
        }

        public PleaDetails(final DisabilityNeeds disabilityNeeds) {
            this.disabilityNeeds = disabilityNeeds.getDisabilityNeeds();
        }


        public Boolean getComeToCourt() {
            return comeToCourt;
        }

        public String getInterpreterLanguage() {
            return interpreterLanguage;
        }

        public void setInterpreterLanguage(final String interpreterLanguage) {
            this.interpreterLanguage = interpreterLanguage;
        }

        public Boolean getSpeakWelsh() {
            return speakWelsh;
        }

        public void setSpeakWelsh(final Boolean speakWelsh) {
            this.speakWelsh = speakWelsh;
        }

        public String getWitnessDispute() {
            return witnessDispute;
        }

        public void setWitnessDispute(final String witnessDispute) {
            this.witnessDispute = witnessDispute;
        }

        public String getWitnessDetails() {
            return witnessDetails;
        }

        public void setWitnessDetails(final String witnessDetails) {
            this.witnessDetails = witnessDetails;
        }

        public String getUnavailability() {
            return unavailability;
        }

        public void setUnavailability(final String unavailability) {
            this.unavailability = unavailability;
        }

        public Boolean getOutstandingFines() {
            return outstandingFines;
        }

        public void setOutstandingFines(Boolean outstandingFines) {
            this.outstandingFines = outstandingFines;
        }

        public String getDisabilityNeeds() {
            return disabilityNeeds;
        }

        public void setDisabilityNeeds(final String disabilityNeeds) {
            this.disabilityNeeds = disabilityNeeds;
        }
    }

    @Embeddable
    public static class Employment {
        @Column(name = "income_payment_frequency")
        @Enumerated(value = EnumType.STRING)
        private Frequency incomePaymentFrequency;
        @Column(name = "income_payment_amount")
        private BigDecimal incomePaymentAmount;
        @Column(name = "employment_status")
        private String employmentStatus;
        @Column(name = "employment_status_details")
        private String employmentStatusDetails;
        @Column(name = "benefits_claimed")
        private Boolean benefitsClaimed;
        @Column(name = "benefits_type")
        private String benefitsType;
        @Column(name = "benefits_deduct_penalty_preference")
        private Boolean benefitsDeductPenaltyPreference;

        @SuppressWarnings({"squid:S1186"})
        public Employment() {
        }

        public Frequency getIncomePaymentFrequency() {
            return incomePaymentFrequency;
        }

        public void setIncomePaymentFrequency(final Frequency incomePaymentFrequency) {
            this.incomePaymentFrequency = incomePaymentFrequency;
        }

        public BigDecimal getIncomePaymentAmount() {
            return incomePaymentAmount;
        }

        public void setIncomePaymentAmount(final BigDecimal incomePaymentAmount) {
            this.incomePaymentAmount = incomePaymentAmount;
        }

        public String getEmploymentStatus() {
            return employmentStatus;
        }

        public void setEmploymentStatus(final String employmentStatus) {
            this.employmentStatus = employmentStatus;
        }

        public String getEmploymentStatusDetails() {
            return employmentStatusDetails;
        }

        public void setEmploymentStatusDetails(final String employmentStatusDetails) {
            this.employmentStatusDetails = employmentStatusDetails;
        }

        public Boolean getBenefitsClaimed() {
            return benefitsClaimed;
        }

        public void setBenefitsClaimed(final Boolean benefitsClaimed) {
            this.benefitsClaimed = benefitsClaimed;
        }

        public String getBenefitsType() {
            return benefitsType;
        }

        public void setBenefitsType(final String benefitsType) {
            this.benefitsType = benefitsType;
        }

        public Boolean getBenefitsDeductPenaltyPreference() {
            return benefitsDeductPenaltyPreference;
        }

        public void setBenefitsDeductPenaltyPreference(final Boolean benefitsDeductPenaltyPreference) {
            this.benefitsDeductPenaltyPreference = benefitsDeductPenaltyPreference;
        }
    }

    @Embeddable
    public static class Employer {
        @Column(name = "employee_reference")
        private String employeeReference;
        @Column(name = "employer_name")
        private String name;
        @Column(name = "employer_phone")
        private String phone;

        @AttributeOverrides({
                @AttributeOverride(name = "address1", column = @Column(name = "employer_address_1")),
                @AttributeOverride(name = "address2", column = @Column(name = "employer_address_2")),
                @AttributeOverride(name = "address3", column = @Column(name = "employer_address_3")),
                @AttributeOverride(name = "address4", column = @Column(name = "employer_address_4")),
                @AttributeOverride(name = "address5", column = @Column(name = "employer_address_5")),
                @AttributeOverride(name = "postcode", column = @Column(name = "employer_postcode"))
        })
        private Address address;

        @SuppressWarnings({"squid:S1186"})
        public Employer() {
        }

        public String getEmployeeReference() {
            return employeeReference;
        }

        public void setEmployeeReference(String employeeReference) {
            this.employeeReference = employeeReference;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }

    }

    @Embeddable
    public static class Outgoings {
        @Column(name = "outgoing_accommodation_amount")
        private BigDecimal accommodationAmount;
        @Column(name = "outgoing_council_tax_amount")
        private BigDecimal councilTaxAmount;
        @Column(name = "outgoing_household_bills_amount")
        private BigDecimal householdBillsAmount;
        @Column(name = "outgoing_travel_expenses_amount")
        private BigDecimal travelExpensesAmount;
        @Column(name = "outgoing_child_maintenance_amount")
        private BigDecimal childMaintenanceAmount;
        @Column(name = "outgoing_other_description")
        private String otherDescription;
        @Column(name = "outgoing_other_amount")
        private BigDecimal otherAmount;

        public BigDecimal getMonthlyAmount() {
            return Stream.of(accommodationAmount,
                            councilTaxAmount,
                            householdBillsAmount,
                            travelExpensesAmount,
                            childMaintenanceAmount,
                            otherAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public BigDecimal getAccommodationAmount() {
            return accommodationAmount;
        }

        public void setAccommodationAmount(BigDecimal accommodationAmount) {
            this.accommodationAmount = accommodationAmount;
        }

        public BigDecimal getCouncilTaxAmount() {
            return councilTaxAmount;
        }

        public void setCouncilTaxAmount(BigDecimal councilTaxAmount) {
            this.councilTaxAmount = councilTaxAmount;
        }

        public BigDecimal getHouseholdBillsAmount() {
            return householdBillsAmount;
        }

        public void setHouseholdBillsAmount(BigDecimal householdBillsAmount) {
            this.householdBillsAmount = householdBillsAmount;
        }

        public BigDecimal getTravelExpensesAmount() {
            return travelExpensesAmount;
        }

        public void setTravelExpensesAmount(BigDecimal travelExpensesAmount) {
            this.travelExpensesAmount = travelExpensesAmount;
        }

        public BigDecimal getChildMaintenanceAmount() {
            return childMaintenanceAmount;
        }

        public void setChildMaintenanceAmount(BigDecimal childMaintenanceAmount) {
            this.childMaintenanceAmount = childMaintenanceAmount;
        }

        public String getOtherDescription() {
            return otherDescription;
        }

        public void setOtherDescription(String otherDescription) {
            this.otherDescription = otherDescription;
        }

        public BigDecimal getOtherAmount() {
            return otherAmount;
        }

        public void setOtherAmount(BigDecimal otherAmount) {
            this.otherAmount = otherAmount;
        }
    }

    @Embeddable
    public static class LegalEntityFinancialMeans {
        @Column(name = "legal_entity_outstanding_fines")
        private Boolean outstandingFines;
        @Column(name = "legal_entity_trading_more_than_12_months")
        private Boolean tradingMoreThan12Months;
        @Column(name = "legal_entity_number_of_employees")
        private Integer numberOfEmployees;
        @Column(name = "legal_entity_gross_turnover")
        private BigDecimal grossTurnover;
        @Column(name = "legal_entity_net_turnover")
        private BigDecimal netTurnover;

        public LegalEntityFinancialMeans(final Boolean outstandingFines, final Boolean tradingMoreThan12Months, final Integer numberOfEmployees, final BigDecimal grossTurnover, final BigDecimal netTurnover) {
            this.outstandingFines = outstandingFines;
            this.tradingMoreThan12Months = tradingMoreThan12Months;
            this.numberOfEmployees = numberOfEmployees;
            this.grossTurnover = grossTurnover;
            this.netTurnover = netTurnover;
        }

        public LegalEntityFinancialMeans() {
        }

        public Boolean getOutstandingFines() {
            return outstandingFines;
        }

        public void setOutstandingFines(Boolean outstandingFines) {
            this.outstandingFines = outstandingFines;
        }

        public Boolean getTradingMoreThan12Months() {
            return tradingMoreThan12Months;
        }

        public void setTradingMoreThan12Months(Boolean tradingMoreThan12Months) {
            this.tradingMoreThan12Months = tradingMoreThan12Months;
        }

        public Integer getNumberOfEmployees() {
            return numberOfEmployees;
        }

        public void setNumberOfEmployees(Integer numberOfEmployees) {
            this.numberOfEmployees = numberOfEmployees;
        }

        public BigDecimal getGrossTurnover() {
            return grossTurnover;
        }

        public void setGrossTurnover(BigDecimal grossTurnover) {
            this.grossTurnover = grossTurnover;
        }

        public BigDecimal getNetTurnover() {
            return netTurnover;
        }

        public void setNetTurnover(BigDecimal netTurnover) {
            this.netTurnover = netTurnover;
        }

    }


}
