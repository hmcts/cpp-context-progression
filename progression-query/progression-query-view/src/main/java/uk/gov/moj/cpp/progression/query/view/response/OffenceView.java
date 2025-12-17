package uk.gov.moj.cpp.progression.query.view.response;



import java.time.LocalDate;
import java.util.UUID;

import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceIndicatedPlea;
import uk.gov.moj.cpp.progression.persistence.entity.OffencePlea;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
@SuppressWarnings({"WeakerAccess", "squid:S1133"})
public class OffenceView {

    private final UUID id;
    private final String offenceCode;
    private final PleaView plea;
    private final String section;
    private final IndicatedPleaView indicatedPlea;
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
    private final LocalDate convictionDate;
    private final Integer count;
    private final int orderIndex;

    public OffenceView(final OffenceDetail offence) {

        this.id = offence.getId();

        this.offenceCode = offence.getCode();
        this.cjsCode = offenceCode;

        this.offenceSequenceNumber = offence.getSequenceNumber()!=null ? offence.getSequenceNumber() : 0;
        this.sequenceNumber = offenceSequenceNumber;
        this.asnSequenceNumber = offenceSequenceNumber;

        this.plea = offence.getOffencePlea() != null ? getPleaView(offence.getOffencePlea()) : null;
        this.indicatedPlea = offence.getOffenceIndicatedPlea() !=null ? getIndicatedPleaview(offence.getOffenceIndicatedPlea()) : null;
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
        this.count=offence.getCount();
        this.convictionDate = offence.getConvictionDate();
        this.orderIndex=offence.getOrderIndex();

    }

    private PleaView getPleaView(final OffencePlea offencePlea) {
        return new PleaView(offencePlea.getId(), offencePlea.getValue(), offencePlea.getPleaDate());
    }

    private IndicatedPleaView getIndicatedPleaview(final OffenceIndicatedPlea offenceIndicatedPlea) {
        return new IndicatedPleaView(offenceIndicatedPlea.getId(),offenceIndicatedPlea.getValue(),offenceIndicatedPlea.getAllocationDecision());
    }

    private String setCprDefendantOffenderCheckDigit(final OffenceDetail offence) {
        return offence.getCpr() != null && offence.getCpr().getDefendantOffender() != null
                        ? offence.getCpr().getDefendantOffender().getCheckDigit()
                        : null;
    }

    private String setCprDefendantOffenderNumber(final OffenceDetail offence) {
        return offence.getCpr() != null && offence.getCpr().getDefendantOffender() != null
                        ? offence.getCpr().getDefendantOffender().getNumber()
                        : null;
    }

    private String setCprDefendantOffenderOrganisationUnit(final OffenceDetail offence) {
        return offence.getCpr() != null && offence.getCpr().getDefendantOffender() != null
                        ? offence.getCpr().getDefendantOffender()
                                        .getOrganisationUnit()
                        : null;
    }

    private String setCprDefendantOffenderYear(final OffenceDetail offence) {
        return offence.getCpr() != null && offence.getCpr().getDefendantOffender() != null
                        ? offence.getCpr().getDefendantOffender().getYear() : null;
    }

    public UUID getId() {
        return id;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public PleaView getPlea() {
        return plea;
    }

    public Integer getOffenceSequenceNumber() {
        return offenceSequenceNumber;
    }

    public String getWording() {
        return wording;
    }

    public IndicatedPleaView getIndicatedPlea() {
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

    public String getSection() {
        return section;
    }

    public Integer getCount() {
        return count;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public int getOrderIndex(){return orderIndex;}
}
