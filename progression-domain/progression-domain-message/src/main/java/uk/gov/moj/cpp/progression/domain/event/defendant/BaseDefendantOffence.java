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
public class BaseDefendantOffence implements Serializable {

    private static final long serialVersionUID = 1L;
    private final UUID id;
    private final String offenceCode;
    private final String wording;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Integer count;
    private final LocalDate convictionDate;

    public BaseDefendantOffence(final UUID id, final String offenceCode, final String wording,
                                final LocalDate startDate, final LocalDate endDate, final Integer count,
                                final LocalDate convictionDate) {
        this.id = id;
        this.offenceCode = offenceCode;
        this.wording = wording;
        this.startDate = startDate;
        this.endDate = endDate;
        this.count = count;
        this.convictionDate = convictionDate;
    }

    public UUID getId() {
        return id;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public String getWording() {
        return wording;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Integer getCount() {
        return count;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }



}
