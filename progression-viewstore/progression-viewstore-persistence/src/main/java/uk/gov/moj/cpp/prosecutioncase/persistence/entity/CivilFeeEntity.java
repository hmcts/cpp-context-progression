package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import uk.gov.moj.cpp.progression.domain.constant.FeeStatus;
import uk.gov.moj.cpp.progression.domain.constant.FeeType;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@SuppressWarnings("squid:S2384")
@Entity
@Table(name = "civil_fee")
public class CivilFeeEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "fee_id", unique = true, nullable = false)
    private UUID feeId;

    @Column(name = "prosecutor_id", unique = true, nullable = false)
    private UUID prosecutorId ;

    @Column(name = "fee_type")
    @Enumerated(EnumType.STRING)
    private FeeType feeType;

    @Column(name = "fee_status")
    @Enumerated(EnumType.STRING)
    private FeeStatus feeStatus;

    @Column(name = "payment_reference")
    private String paymentReference;

    public CivilFeeEntity(final UUID feeId, final FeeType feeType, final FeeStatus feeStatus, final String paymentReference) {
        this.feeId = feeId;
        this.feeType = feeType;
        this.feeStatus = feeStatus;
        this.paymentReference = paymentReference;
    }

    public CivilFeeEntity() {
    }

    public UUID getFeeId() {
        return feeId;
    }

    public void setFeeId(final UUID feeId) {
        this.feeId = feeId;
    }

    public UUID getProsecutorId() {
        return prosecutorId;
    }

    public void setProsecutorId(UUID prosecutorId) {
        this.prosecutorId = prosecutorId;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public void setFeeType(final FeeType feeType) {
        this.feeType = feeType;
    }

    public FeeStatus getFeeStatus() {
        return feeStatus;
    }

    public void setFeeStatus(final FeeStatus feeStatus) {
        this.feeStatus = feeStatus;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }
}
