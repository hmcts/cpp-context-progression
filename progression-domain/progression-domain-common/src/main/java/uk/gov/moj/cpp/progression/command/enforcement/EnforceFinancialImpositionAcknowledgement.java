package uk.gov.moj.cpp.progression.command.enforcement;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

@SuppressWarnings("squid:S1067")
public class EnforceFinancialImpositionAcknowledgement implements Serializable {
    private static final long serialVersionUID = -2221378401462966080L;

    private Acknowledgement acknowledgement;

    private ExportStatus exportStatus;

    private String originator;

    private UUID requestId;

    private ZonedDateTime updated;

    private UUID materialId;

    public EnforceFinancialImpositionAcknowledgement(final Acknowledgement acknowledgement, final ExportStatus exportStatus, final String originator, final UUID requestId, UUID materialId, final ZonedDateTime updated) {
        this.acknowledgement = acknowledgement;
        this.exportStatus = exportStatus;
        this.originator = originator;
        this.requestId = requestId;
        this.materialId = materialId;
        this.updated = updated;
    }

    public Acknowledgement getAcknowledgement() {
        return acknowledgement;
    }

    public ExportStatus getExportStatus() {
        return exportStatus;
    }

    public String getOriginator() {
        return originator;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public ZonedDateTime getUpdated() {
        return updated;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public static Builder enforceFinancialImpositionAcknowledgement() {
        return new EnforceFinancialImpositionAcknowledgement.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final EnforceFinancialImpositionAcknowledgement that = (EnforceFinancialImpositionAcknowledgement) obj;

        return java.util.Objects.equals(this.acknowledgement, that.acknowledgement) &&
                java.util.Objects.equals(this.exportStatus, that.exportStatus) &&
                java.util.Objects.equals(this.originator, that.originator) &&
                java.util.Objects.equals(this.requestId, that.requestId) &&
                java.util.Objects.equals(this.updated, that.updated);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(acknowledgement, exportStatus, originator, requestId, updated);
    }

    @Override
    public String toString() {
        return "EnforceFinancialImpositionAcknowledgement{" +
                "acknowledgement='" + acknowledgement + "'," +
                "exportStatus='" + exportStatus + "'," +
                "originator='" + originator + "'," +
                "requestId='" + requestId + "'," +
                "updated='" + updated + "'" +
                "}";
    }

    public EnforceFinancialImpositionAcknowledgement setAcknowledgement(Acknowledgement acknowledgement) {
        this.acknowledgement = acknowledgement;
        return this;
    }

    public EnforceFinancialImpositionAcknowledgement setExportStatus(ExportStatus exportStatus) {
        this.exportStatus = exportStatus;
        return this;
    }

    public EnforceFinancialImpositionAcknowledgement setOriginator(String originator) {
        this.originator = originator;
        return this;
    }

    public EnforceFinancialImpositionAcknowledgement setRequestId(UUID requestId) {
        this.requestId = requestId;
        return this;
    }

    public EnforceFinancialImpositionAcknowledgement setUpdated(ZonedDateTime updated) {
        this.updated = updated;
        return this;
    }

    public static class Builder {
        private Acknowledgement acknowledgement;

        private ExportStatus exportStatus;

        private String originator;

        private UUID requestId;

        private ZonedDateTime updated;

        private UUID materialId;

        public Builder withAcknowledgement(final Acknowledgement acknowledgement) {
            this.acknowledgement = acknowledgement;
            return this;
        }

        public Builder withExportStatus(final ExportStatus exportStatus) {
            this.exportStatus = exportStatus;
            return this;
        }

        public Builder withOriginator(final String originator) {
            this.originator = originator;
            return this;
        }

        public Builder withRequestId(final UUID requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder withMaterialId(final UUID materialId) {
            this.materialId = materialId;
            return this;
        }

        public Builder withUpdated(final ZonedDateTime updated) {
            this.updated = updated;
            return this;
        }

        public EnforceFinancialImpositionAcknowledgement build() {
            return new EnforceFinancialImpositionAcknowledgement(acknowledgement, exportStatus, originator, requestId, materialId, updated);
        }
    }
}
