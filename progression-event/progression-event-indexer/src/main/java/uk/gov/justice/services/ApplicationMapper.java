package uk.gov.justice.services;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.services.unifiedsearch.client.domain.Application;

import java.time.LocalDate;

public class ApplicationMapper {
    public Application transform(final CourtApplication courtApplication) {
        final Application application = new Application();
        application.setApplicationId(courtApplication.getId().toString());
        application.setApplicationReference(courtApplication.getApplicationReference());

        final LocalDate dueDate = courtApplication.getDueDate();
        if (dueDate != null) {
            application.setDueDate(dueDate.toString());
        }

        final LocalDate applicationReceivedDate = courtApplication.getApplicationReceivedDate();
        if (applicationReceivedDate != null) {
            application.setReceivedDate(applicationReceivedDate.toString());
        }
        final LocalDate applicationDecisionSoughtByDate = courtApplication.getApplicationDecisionSoughtByDate();
        if (applicationDecisionSoughtByDate != null) {
            application.setDecisionDate(applicationDecisionSoughtByDate.toString());
        }
        final CourtApplicationType type = courtApplication.getType();
        if (type != null) {
            application.setApplicationType(type.getApplicationType());
        }
        return application;
    }
}
