package uk.gov.moj.cpp.progression.query.view.response;



import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public class OffenceView {

    private final UUID id;
    private final String offenceCode;
    private final String plea;
    private final String section;
    private final String indicatedPlea;
    private final Integer offenceSequenceNumber;
    private final String wording;
    private final String policeOffenceId;
    private final String cprDefendantOffenderYear;
    private final String cprDefendantOffenderOrganisationUnit;
    private final String cprDefendantOffenderNumber;
    private final String cprDefendantOffenderCheckDigit;
    private final String cjsCode;
    private final Integer sequenceNumber;
    private final Integer asnSequenceNumber;
    private final String reason;
    private final String description;
    private final String category;
    private final LocalDate arrestDate;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDate chargeDate;
    private final Boolean pendingWithdrawal;
    private BigDecimal compensation;
    private String prosecutionFacts;
    private Integer count;

    public OffenceView(final OffenceDetail offence) {

        this.id = offence.getId();

        this.offenceCode = offence.getCode();
        this.cjsCode = offenceCode;

        this.offenceSequenceNumber = offence.getSequenceNumber()!=null ? offence.getSequenceNumber() : 0;
        this.sequenceNumber = offenceSequenceNumber;
        this.asnSequenceNumber = offenceSequenceNumber;

        this.plea = offence.getPlea();
        this.indicatedPlea = offence.getIndicatedPlea();
        this.section=offence.getSection();
        this.wording = offence.getWording();
        this.policeOffenceId = offence.getPoliceOffenceId();
        this.cprDefendantOffenderYear = setCprDefendantOffenderYear(offence);
        this.cprDefendantOffenderOrganisationUnit = setCprDefendantOffenderOrganisationUnit(offence);
        this.cprDefendantOffenderNumber = setCprDefendantOffenderNumber(offence);
        this.cprDefendantOffenderCheckDigit = setCprDefendantOffenderCheckDigit(offence);
        this.reason = offence.getReason();
        this.description = offence.getDescription();
        this.category = offence.getCategory();
        this.arrestDate = offence.getArrestDate();
        this.startDate = offence.getStartDate();
        this.endDate=offence.getEndDate();
        this.chargeDate = offence.getChargeDate();
        this.pendingWithdrawal = offence.getPendingWithdrawal();
        this.compensation = offence.getCompensation();
        this.prosecutionFacts = offence.getProsecutionFacts();
        this.count=offence.getCount();

    }

    private String setCprDefendantOffenderCheckDigit(OffenceDetail offence) {
        return offence.getCpr() != null && offence.getCpr().getDefendantOffender() != null
                        ? offence.getCpr().getDefendantOffender().getCheckDigit()
                        : null;
    }

    private String setCprDefendantOffenderNumber(OffenceDetail offence) {
        return offence.getCpr() != null && offence.getCpr().getDefendantOffender() != null
                        ? offence.getCpr().getDefendantOffender().getNumber()
                        : null;
    }

    private String setCprDefendantOffenderOrganisationUnit(OffenceDetail offence) {
        return offence.getCpr() != null && offence.getCpr().getDefendantOffender() != null
                        ? offence.getCpr().getDefendantOffender()
                                        .getOrganisationUnit()
                        : null;
    }

    private String setCprDefendantOffenderYear(OffenceDetail offence) {
        return offence.getCpr() != null && offence.getCpr().getDefendantOffender() != null
                        ? offence.getCpr().getDefendantOffender().getYear() : null;
    }

    public UUID getId() {
        return id;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public String getPlea() {
        return plea;
    }

    public Integer getOffenceSequenceNumber() {
        return offenceSequenceNumber;
    }

    public String getWording() {
        return wording;
    }

    public String getIndicatedPlea() {
        return indicatedPlea;
    }

    public String getPoliceOffenceId() {
        return policeOffenceId;
    }

    public String getCprDefendantOffenderYear() {
        return cprDefendantOffenderYear;
    }

    public String getCprDefendantOffenderOrganisationUnit() {
        return cprDefendantOffenderOrganisationUnit;
    }

    public String getCprDefendantOffenderNumber() {
        return cprDefendantOffenderNumber;
    }

    public String getCprDefendantOffenderCheckDigit() {
        return cprDefendantOffenderCheckDigit;
    }

    public String getCjsCode() {
        return cjsCode;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public Integer getAsnSequenceNumber() {
        return asnSequenceNumber;
    }

    public String getReason() {
        return reason;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public LocalDate getArrestDate() {
        return arrestDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }

    public Boolean getPendingWithdrawal() {
        return pendingWithdrawal;
    }

    public BigDecimal getCompensation() {
        return compensation;
    }

    public String getProsecutionFacts() {
        return prosecutionFacts;
    }

    public String getSection() {
        return section;
    }

    public Integer getCount() {
        return count;
    }
}
