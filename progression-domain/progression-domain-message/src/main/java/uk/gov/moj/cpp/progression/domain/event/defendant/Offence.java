package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public final class Offence implements Serializable {


    private static final long serialVersionUID = 1828473390590503938L;
    private final UUID id;
    private final String policeOffenceId;
    private final CPR cpr;
    private final String asnSequenceNumber;
    private final String cjsCode;
    private final String reason;
    private final String description;
    private final String wording;
    private final String category;
    private final LocalDate arrestDate;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDate chargeDate;


    @SuppressWarnings("squid:S00107")
    public Offence(final UUID id,
                   final String policeOffenceId,
                   final CPR cpr,
                   final String asnSequenceNumber,
                   final String cjsCode,
                   final String reason,
                   final String description,
                   final String wording,
                   final String category,
                   final LocalDate arrestDate,
                   final LocalDate startDate,
                   final LocalDate endDate,
                   final LocalDate chargeDate) {

        this.id = id;
        this.policeOffenceId = policeOffenceId;
        this.cpr = cpr;
        this.asnSequenceNumber = asnSequenceNumber;
        this.cjsCode = cjsCode;
        this.reason = reason;
        this.description = description;
        this.wording = wording;
        this.category = category;
        this.arrestDate = arrestDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.chargeDate = chargeDate;
    }

    public UUID getId() {
        return id;
    }

    public String getPoliceOffenceId() {
        return policeOffenceId;
    }
    
    public CPR getCpr() {
        return cpr;
    }

    public String getAsnSequenceNumber() {
        return asnSequenceNumber;
    }

    public String getCjsCode() {
        return cjsCode;
    }

    public String getReason() {
        return reason;
    }

    public String getDescription() {
        return description;
    }

    public String getWording() {
        return wording;
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
}
