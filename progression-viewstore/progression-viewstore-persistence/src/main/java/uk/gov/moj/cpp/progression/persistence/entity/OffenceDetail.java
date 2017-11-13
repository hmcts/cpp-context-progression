package uk.gov.moj.cpp.progression.persistence.entity;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "offence")
public class OffenceDetail implements Serializable {
    @Column(name = "id")
    @Id
    private UUID id;

    @Column(name = "code")
    private String code;

    @Column(name = "plea")
    private String plea;

    @Column(name = "seq_no")
    private Integer sequenceNumber;

    @Column(name = "wording")
    private String wording;


    @Column(name = "indicated_plea")
    private String indicatedPlea;

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



    @Column(name = "pending_withdrawal")
    private Boolean pendingWithdrawal;

    @ManyToOne
    @JoinColumn(name = "defendant_id")
    private Defendant defendant;

    @Column(name = "libra_offence_date_code")
    private int libraOffenceDateCode;

    @Column(name = "witness_statement")
    private String witnessStatement;

    @Column(name = "prosecution_facts")
    private String prosecutionFacts;

    @Column(name = "compensation")
    private BigDecimal compensation;

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
        this.plea = builder.plea;
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
        this.pendingWithdrawal = builder.pendingWithdrawal;
        this.witnessStatement = builder.witnessStatement;
        this.prosecutionFacts = builder.prosecutionFacts;
        this.libraOffenceDateCode = builder.libraOffenceDateCode;
        this.compensation = builder.compensation;
        this.indicatedPlea = builder.indicatedPlea;
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

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPlea() {
        return plea;
    }

    public void setPlea(String plea) {
        this.plea = plea;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getWording() {
        return wording;
    }

    public void setWording(String wording) {
        this.wording = wording;
    }


    public String getIndicatedPlea() {
        return indicatedPlea;
    }

    public void setIndicatedPlea(String indicatedPlea) {
        this.indicatedPlea = indicatedPlea;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getPoliceOffenceId() {
        return policeOffenceId;
    }

    public void setPoliceOffenceId(String policeOffenceId) {
        this.policeOffenceId = policeOffenceId;
    }

    public CPRDetails getCpr() {
        return cpr;
    }

    public void setCpr(CPRDetails cpr) {
        this.cpr = cpr;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getArrestDate() {
        return arrestDate;
    }

    public void setArrestDate(LocalDate arrestDate) {
        this.arrestDate = arrestDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }

    public void setChargeDate(LocalDate chargeDate) {
        this.chargeDate = chargeDate;
    }


    public Boolean getPendingWithdrawal() {
        return pendingWithdrawal;
    }

    public void setPendingWithdrawal(Boolean pendingWithdrawal) {
        this.pendingWithdrawal = pendingWithdrawal;
    }

    public Defendant getDefendant() {
        return defendant;
    }

    public void setDefendant(Defendant defendantDetail) {
        this.defendant = defendantDetail;
    }

    public String getWitnessStatement() {
        return witnessStatement;
    }

    public void setWitnessStatement(String witnessStatement) {
        this.witnessStatement = witnessStatement;
    }

    public String getProsecutionFacts() {
        return prosecutionFacts;
    }

    public void setProsecutionFacts(String prosecutionFacts) {
        this.prosecutionFacts = prosecutionFacts;
    }

    public int getLibraOffenceDateCode() {
        return libraOffenceDateCode;
    }

    public void setLibraOffenceDateCode(int libraOffenceDateCode) {
        this.libraOffenceDateCode = libraOffenceDateCode;
    }

    public BigDecimal getCompensation() {
        return compensation;
    }

    public void setCompensation(BigDecimal compensation) {
        this.compensation = compensation;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OffenceDetail)) {
            return false;
        }

        OffenceDetail that = (OffenceDetail) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static class OffenceDetailBuilder {

        private UUID id;
        private String code;
        private String plea;
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
        private Boolean pendingWithdrawal;
        private String witnessStatement;
        private String prosecutionFacts;
        private int libraOffenceDateCode;
        private BigDecimal compensation;
        private String indicatedPlea;
        private String section;
        private int orderIndex;
        private Integer count;


        public OffenceDetail build() {
            return new OffenceDetail(this);
        }

        public OffenceDetailBuilder setId(UUID id) {
            this.id = id;
            return this;
        }

        public OffenceDetailBuilder setCode(String code) {
            this.code = code;
            return this;
        }

        public OffenceDetailBuilder setPlea(String plea) {
            this.plea = plea;
            return this;
        }


        public OffenceDetailBuilder setSequenceNumber(Integer sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
            return this;
        }

        public OffenceDetailBuilder setWording(String wording) {
            this.wording = wording;
            return this;
        }


        public OffenceDetailBuilder setPoliceOffenceId(String policeOffenceId) {
            this.policeOffenceId = policeOffenceId;
            return this;
        }


        public OffenceDetailBuilder setCpr(CPRDetails cpr) {
            this.cpr = cpr;
            return this;
        }

        public OffenceDetailBuilder setReason(String reason) {
            this.reason = reason;
            return this;
        }

        public OffenceDetailBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public OffenceDetailBuilder setCategory(String category) {
            this.category = category;
            return this;
        }

        public OffenceDetailBuilder setArrestDate(LocalDate arrestDate) {
            this.arrestDate = arrestDate;
            return this;
        }

        public OffenceDetailBuilder setStartDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public OffenceDetailBuilder setEndDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public OffenceDetailBuilder setChargeDate(LocalDate chargeDate) {
            this.chargeDate = chargeDate;
            return this;
        }


        public OffenceDetailBuilder setPendingWithdrawal(boolean pendingWithdrawal) {
            this.pendingWithdrawal = pendingWithdrawal;
            return this;
        }

        public OffenceDetailBuilder withWitnessStatement(String witnessStatement) {
            this.witnessStatement = witnessStatement;
            return this;
        }

        public OffenceDetailBuilder withProsecutionFacts(String prosecutionFacts) {
            this.prosecutionFacts = prosecutionFacts;
            return this;
        }

        public OffenceDetailBuilder withLibraOffenceDateCode(int libraOffenceDateCode) {
            this.libraOffenceDateCode = libraOffenceDateCode;
            return this;
        }

        public OffenceDetailBuilder withCompensation(BigDecimal compensation) {
            this.compensation = compensation;
            return this;
        }

        public OffenceDetailBuilder withIndicatedPlea(String indicatedPlea) {
            this.indicatedPlea = indicatedPlea;
            return this;
        }

        public OffenceDetailBuilder withSection(String section) {
            this.section = section;
            return this;
        }

        public OffenceDetailBuilder withOrderIndex(int orderIndex) {
            this.orderIndex = orderIndex;
            return this;
        }

        public OffenceDetailBuilder withCount(Integer count) {
            this.count = count;
            return this;
        }
    }

}
