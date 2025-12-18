package uk.gov.moj.cpp.progression.processor;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.domain.helper.JsonHelper.removeProperty;
import static uk.gov.moj.cpp.progression.domain.helper.JsonHelper.addProperty;

import uk.gov.justice.core.courts.ApplicationDefenceOrganisationChanged;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.query.laa.HearingSummary;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.application.ApplicationCaseDefendantOrganisation;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ApplicationDefenceOrganisationChangedProcessor {

    protected static final String PUBLIC_CASE_DEFENDANT_CHANGED = "public.progression.case-defendant-changed";
    protected static final String PUBLIC_APPLICATION_ORGANISATION_CHANGED = "public.progression.application-organisation-changed";
    protected static final String UPDATE_APPLICATION_REP_ORDER_FOR_HEARING = "progression.command.handler.update-application-rep-order-for-hearing";
    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantDefenceOrganisationChangedProcessor.class.getCanonicalName());

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("progression.event.application-defence-organisation-changed")
    public void handleApplicationDefenceOrganisationChanged(final JsonEnvelope jsonEnvelope) {
        final ApplicationDefenceOrganisationChanged applicationDefenceOrganisationChanged = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ApplicationDefenceOrganisationChanged.class);
        LOGGER.debug("Application defence organisation changed for applicationId: {}", applicationDefenceOrganisationChanged.getApplicationId());
        final List<ApplicationCaseDefendantOrganisation> applicationCaseDefendantOrganisationList = applicationDefenceOrganisationChanged.getApplicationCaseDefendantOrganisations();
        final JsonEnvelope jsonUpdatedEnvelope = removeApplicationCaseDefendantOrganisationsElementFromEnvelope(jsonEnvelope);

        if (isNotEmpty(applicationCaseDefendantOrganisationList)) {
            applicationCaseDefendantOrganisationList.forEach(applicationCaseDefendantOrganisation -> {
                handleDefendantDefenceOrganisationChanged(jsonEnvelope, applicationCaseDefendantOrganisation.getCaseId(), applicationCaseDefendantOrganisation.getDefendantId(), applicationDefenceOrganisationChanged.getAssociatedDefenceOrganisation());
            });
        }

        // For Hearing and Listing contexts
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_APPLICATION_ORGANISATION_CHANGED).apply(jsonUpdatedEnvelope.payloadAsJsonObject()));
        final Optional<List<HearingSummary>> hearingSummaryListOptional = progressionService.getHearingsForApplication(applicationDefenceOrganisationChanged.getApplicationId());
        if (hearingSummaryListOptional.isEmpty() || isEmpty(hearingSummaryListOptional.get())) {
            return;
        }

        final List<HearingSummary> hearingSummaryList = hearingSummaryListOptional.get();
        hearingSummaryList.stream().forEach(hearingSummary -> {
            final JsonObject jsonObject = updateJsonObjectWithHearingId(jsonUpdatedEnvelope,hearingSummary.getHearingId());
            sender.send(enveloper.withMetadataFrom(jsonEnvelope, UPDATE_APPLICATION_REP_ORDER_FOR_HEARING).apply(jsonObject));
        });
    }

    private void handleDefendantDefenceOrganisationChanged(final JsonEnvelope jsonEnvelope, final UUID caseId, final UUID defendantId, final AssociatedDefenceOrganisation associatedDefenceOrganisation) {
        LOGGER.debug("Defendant defence organisation changed for caseId: {}", caseId);
        final Optional<JsonObject> optionalProsCase = progressionService.getProsecutionCaseDetailById(jsonEnvelope, caseId.toString());
        final JsonObject prosecutionCaseJson = optionalProsCase.orElseThrow(() -> new RuntimeException("Prosecution Case not found")).getJsonObject("prosecutionCase");
        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final Optional<Defendant> optionalDefendant = prosecutionCase.getDefendants().stream()
                .filter(defendant -> defendant.getId().equals(defendantId))
                .findFirst();
        if (optionalDefendant.isPresent()) {
            final JsonObject publicEventPayload = JsonObjects.createObjectBuilder()
                    .add("defendant", objectToJsonObjectConverter.convert(updateDefendant(caseId, defendantId, associatedDefenceOrganisation, optionalDefendant.get())))
                    .build();

            // For Defence context
            sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_CASE_DEFENDANT_CHANGED).apply(publicEventPayload));
        }
    }

    private DefendantUpdate updateDefendant(final UUID caseId, final UUID defendantId, final AssociatedDefenceOrganisation associatedDefenceOrganisation, final Defendant originalDefendant) {
        return DefendantUpdate.defendantUpdate()
                .withId(defendantId)
                .withMasterDefendantId(originalDefendant.getMasterDefendantId())
                .withProsecutionCaseId(caseId)
                .withAssociatedDefenceOrganisation(associatedDefenceOrganisation)
                .withPersonDefendant(originalDefendant.getPersonDefendant())
                .withAliases(originalDefendant.getAliases())
                .withAssociatedPersons(originalDefendant.getAssociatedPersons())
                .withDefenceOrganisation(originalDefendant.getDefenceOrganisation())
                .withLegalEntityDefendant(originalDefendant.getLegalEntityDefendant())
                .withMitigation(originalDefendant.getMitigation())
                .withMitigationWelsh(originalDefendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(originalDefendant.getNumberOfPreviousConvictionsCited())
                .withPncId(originalDefendant.getPncId())
                .withProsecutionAuthorityReference(originalDefendant.getProsecutionAuthorityReference())
                .withWitnessStatement(originalDefendant.getWitnessStatement())
                .withWitnessStatementWelsh(originalDefendant.getWitnessStatementWelsh())
                .withIsYouth(originalDefendant.getIsYouth())
                .build();
    }

    private JsonEnvelope removeApplicationCaseDefendantOrganisationsElementFromEnvelope(final JsonEnvelope jsonEnvelope) {
        return envelopeFrom(jsonEnvelope.metadata(), removeProperty(jsonEnvelope.payloadAsJsonObject(), "applicationCaseDefendantOrganisations"));
    }

    private JsonObject updateJsonObjectWithHearingId(final JsonEnvelope jsonEnvelope, final UUID hearingId) {
        final JsonObject jsonObject = jsonEnvelope.payloadAsJsonObject();
        return addProperty(jsonObject, "hearingId", hearingId.toString());
    }
}
