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
public class OffenceForDefendant implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID id;

    private final String offenceCode;

    private final OffencePlea offencePlea;

    private final OffenceIndicatedPlea offenceIndicatedPlea;

    private final LocalDate convictionDate;

    private final String section;

    private final String wording;

    private final LocalDate startDate;

    private final LocalDate endDate;

    private final int orderIndex;

    private final Integer count;

    public UUID getId() {
        return id;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public OffenceIndicatedPlea getOffenceIndicatedPlea() {
        return offenceIndicatedPlea;
    }

    public String getSection() {
        return section;
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

    public int getOrderIndex() {
        return orderIndex;
    }

    public Integer getCount() {
        return count;
    }

    public OffencePlea getOffencePlea() {
        return offencePlea;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }


    @SuppressWarnings("squid:S00107")
    public OffenceForDefendant(final UUID id, final String offenceCode, final String section,
                               final String wording, final LocalDate startDate, final LocalDate endDate, final int orderIndex, final Integer count, final OffencePlea offencePlea, final OffenceIndicatedPlea offenceIndicatedPlea, final LocalDate convictionDate) {
        this.id = id;
        this.offenceCode = offenceCode;
        this.offencePlea = offencePlea;
        this.section = section;
        this.wording = wording;
        this.startDate = startDate;
        this.endDate = endDate;
        this.orderIndex = orderIndex;
        this.count = count;
        this.offenceIndicatedPlea = offenceIndicatedPlea;
        this.convictionDate = convictionDate;
    }
}
