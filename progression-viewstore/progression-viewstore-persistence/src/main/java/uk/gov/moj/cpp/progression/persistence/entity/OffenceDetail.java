package uk.gov.moj.cpp.progression.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
/**
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@Entity
@Table(name = "offence")
public class OffenceDetail implements Serializable {
    @Column(name = "id")
    @Id
    private UUID id;

    @Column(name = "code")
    private String code;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "plea_id")
    private OffencePlea offencePlea;

    @Column(name = "seq_no")
    private Integer sequenceNumber;

    @Column(name = "wording")
    private String wording;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "indicated_plea_id")
    private OffenceIndicatedPlea offenceIndicatedPlea;

    @Column(name = "section")
    private String section;

    @Column(name = "police_offence_id")
    private String policeOffenceId;

    @Embedded
    private CPRDetails cpr;

    @Column(name = "reason")
    private String reason;

    @Column(name = "description")
    private String description;

    @Column(name = "category")
    private String category;

    @Column(name = "arrest_date")
    private LocalDate arrestDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "charge_date")
    private LocalDate chargeDate;

    @Column(name = "conviction_date")
    private LocalDate convictionDate;

    @ManyToOne
    @JoinColumn(name = "defendant_id")
    private Defendant defendant;

    @Column(name = "order_index")
    private int orderIndex;

    @Column(name = "count")
    private Integer count;

    public OffenceDetail() {
        super();
    }


    private OffenceDetail(final OffenceDetailBuilder builder) {
        this.id = builder.id;
        this.code = builder.code;
        this.offencePlea = builder.offencePlea;
        this.sequenceNumber = builder.sequenceNumber;
        this.wording = builder.wording;
        this.policeOffenceId = builder.policeOffenceId;
        this.cpr = builder.cpr;
        this.reason = builder.reason;
        this.description = builder.description;
        this.category = builder.category;
        this.arrestDate = builder.arrestDate;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.chargeDate = builder.chargeDate;
        this.convictionDate = builder.convictionDate;
        this.offenceIndicatedPlea = builder.offenceIndicatedPlea;
        this.section = builder.section;
        this.orderIndex = builder.orderIndex;
        this.count = builder.count;

    }

    public static OffenceDetailBuilder builder() {
        return new OffenceDetailBuilder();
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public OffencePlea getOffencePlea() {
        return offencePlea;
    }

    public void setOffencePlea(final OffencePlea offencePlea) {
        this.offencePlea = offencePlea;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(final Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getWording() {
        return wording;
    }

    public void setWording(final String wording) {
        this.wording = wording;
    }


    public OffenceIndicatedPlea getOffenceIndicatedPlea() {
        return offenceIndicatedPlea;
    }

    public void setOffenceIndicatedPlea(final OffenceIndicatedPlea offenceIndicatedPlea) {
        this.offenceIndicatedPlea = offenceIndicatedPlea;
    }

    public String getSection() {
        return section;
    }

    public void setSection(final String section) {
        this.section = section;
    }

    public String getPoliceOffenceId() {
        return policeOffenceId;
    }

    public void setPoliceOffenceId(final String policeOffenceId) {
        this.policeOffenceId = policeOffenceId;
    }

    public CPRDetails getCpr() {
        return cpr;
    }

    public void setCpr(final CPRDetails cpr) {
        this.cpr = cpr;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public LocalDate getArrestDate() {
        return arrestDate;
    }

    public void setArrestDate(final LocalDate arrestDate) {
        this.arrestDate = arrestDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }

    public void setChargeDate(final LocalDate chargeDate) {
        this.chargeDate = chargeDate;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public void setConvictionDate(final LocalDate convictionDate) {
        this.convictionDate = convictionDate;
    }

    public Defendant getDefendant() {
        return defendant;
    }

    public void setDefendant(final Defendant defendantDetail) {
        this.defendant = defendantDetail;
    }
    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(final int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(final Integer count) {
        this.count = count;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OffenceDetail)) {
            return false;
        }

        final OffenceDetail that = (OffenceDetail) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static class OffenceDetailBuilder {

        private UUID id;
        private String code;
        private OffencePlea offencePlea;
        private Integer sequenceNumber;
        private String wording;
        private String policeOffenceId;
        private CPRDetails cpr;
        private String reason;
        private String description;
        private String category;
        private LocalDate arrestDate;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate chargeDate;
        private LocalDate convictionDate;
        private OffenceIndicatedPlea offenceIndicatedPlea;
        private String section;
        private int orderIndex;
        private Integer count;


        public OffenceDetail build() {
            return new OffenceDetail(this);
        }

        public OffenceDetailBuilder setId(final UUID id) {
            this.id = id;
            return this;
        }

        public OffenceDetailBuilder setCode(final String code) {
            this.code = code;
            return this;
        }

        public OffenceDetailBuilder setOffencePlea(final OffencePlea offencePlea) {
            this.offencePlea = offencePlea;
            return this;
        }


        public OffenceDetailBuilder setSequenceNumber(final Integer sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
            return this;
        }

        public OffenceDetailBuilder setWording(final String wording) {
            this.wording = wording;
            return this;
        }


        public OffenceDetailBuilder setPoliceOffenceId(final String policeOffenceId) {
            this.policeOffenceId = policeOffenceId;
            return this;
        }


        public OffenceDetailBuilder setCpr(final CPRDetails cpr) {
            this.cpr = cpr;
            return this;
        }

        public OffenceDetailBuilder setReason(final String reason) {
            this.reason = reason;
            return this;
        }

        public OffenceDetailBuilder setDescription(final String description) {
            this.description = description;
            return this;
        }

        public OffenceDetailBuilder setCategory(final String category) {
            this.category = category;
            return this;
        }

        public OffenceDetailBuilder setArrestDate(final LocalDate arrestDate) {
            this.arrestDate = arrestDate;
            return this;
        }

        public OffenceDetailBuilder setStartDate(final LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public OffenceDetailBuilder setEndDate(final LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public OffenceDetailBuilder setChargeDate(final LocalDate chargeDate) {
            this.chargeDate = chargeDate;
            return this;
        }

        public OffenceDetailBuilder setConvictionDate(final LocalDate convictionDate) {
            this.convictionDate = convictionDate;
            return this;
        }
        
        public OffenceDetailBuilder withOffenceIndicatedPlea(final OffenceIndicatedPlea offenceIndicatedPlea) {
            this.offenceIndicatedPlea = offenceIndicatedPlea;
            return this;
        }

        public OffenceDetailBuilder withSection(final String section) {
            this.section = section;
            return this;
        }

        public OffenceDetailBuilder withOrderIndex(final int orderIndex) {
            this.orderIndex = orderIndex;
            return this;
        }

        public OffenceDetailBuilder withCount(final Integer count) {
            this.count = count;
            return this;
        }
    }

}
