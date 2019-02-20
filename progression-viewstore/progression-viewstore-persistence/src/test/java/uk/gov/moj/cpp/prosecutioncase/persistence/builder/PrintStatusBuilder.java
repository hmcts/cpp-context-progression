package uk.gov.moj.cpp.prosecutioncase.persistence.builder;

import uk.gov.moj.cpp.progression.domain.constant.PrintStatusType;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PrintStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

public final class PrintStatusBuilder {
    private UUID caseId;
    private UUID notificationId;
    private UUID materialId;
    private String errorMessage;
    private Integer statusCode;
    private PrintStatusType status;
    private ZonedDateTime updated;

    private PrintStatusBuilder() {
    }

    public static PrintStatusBuilder printStatus() {
        return new PrintStatusBuilder();
    }

    public PrintStatusBuilder withCaseId(final UUID caseId) {
        this.caseId = caseId;
        return this;
    }

    public PrintStatusBuilder withNotificationId(final UUID notificationId) {
        this.notificationId = notificationId;
        return this;
    }

    public PrintStatusBuilder withMaterialId(final UUID materialId) {
        this.materialId = materialId;
        return this;
    }

    public PrintStatusBuilder withErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public PrintStatusBuilder withStatusCode(final Integer statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public PrintStatusBuilder withStatus(final PrintStatusType status) {
        this.status = status;
        return this;
    }

    public PrintStatusBuilder withUpdated(final ZonedDateTime updated) {
        this.updated = updated;
        return this;
    }

    public PrintStatus build() {

        return new PrintStatus(
                caseId,
                notificationId,
                materialId,
                errorMessage,
                statusCode,
                status,
                updated
        );
    }
}
