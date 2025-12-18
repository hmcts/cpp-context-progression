package uk.gov.moj.cpp.progression.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantDefenceOrganisationChanged;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import java.util.Optional;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

@ServiceComponent(EVENT_PROCESSOR)
public class DefendantDefenceOrganisationChangedProcessor {

    protected static final String PUBLIC_CASE_DEFENDANT_CHANGED = "public.progression.case-defendant-changed";
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

    @Handles("progression.event.defendant-defence-organisation-changed")
    public void handleDefendantDefenceOrganisationChanged(final JsonEnvelope jsonEnvelope) {
        final DefendantDefenceOrganisationChanged defendantDefenceOrganisationChanged = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), DefendantDefenceOrganisationChanged.class);
        LOGGER.debug("Defendant defence organisation changed for caseId: {}", defendantDefenceOrganisationChanged.getProsecutionCaseId());
        final Optional<JsonObject> optionalProsCase = progressionService.getProsecutionCaseDetailById(jsonEnvelope, defendantDefenceOrganisationChanged.getProsecutionCaseId().toString());
        final JsonObject prosecutionCaseJson = optionalProsCase.orElseThrow(() -> new RuntimeException("Prosecution Case not found")).getJsonObject("prosecutionCase");
        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final Optional<Defendant> optionalDefendant = prosecutionCase.getDefendants().stream()
                .filter(defendant -> defendant.getId().equals(defendantDefenceOrganisationChanged.getDefendantId()))
                .findFirst();
        if (optionalDefendant.isPresent()) {
            final JsonObject publicEventPayload = JsonObjects.createObjectBuilder()
                    .add("defendant", objectToJsonObjectConverter.convert(updateDefendant(defendantDefenceOrganisationChanged, optionalDefendant.get())))
                    .build();
            sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_CASE_DEFENDANT_CHANGED).apply(publicEventPayload));

        }

    }

    private DefendantUpdate updateDefendant(final DefendantDefenceOrganisationChanged defendantDefenceOrganisationChanged, final Defendant originalDefendant) {
        return DefendantUpdate.defendantUpdate()
                .withId(defendantDefenceOrganisationChanged.getDefendantId())
                .withMasterDefendantId(originalDefendant.getMasterDefendantId())
                .withProsecutionCaseId(defendantDefenceOrganisationChanged.getProsecutionCaseId())
                .withAssociatedDefenceOrganisation(defendantDefenceOrganisationChanged.getAssociatedDefenceOrganisation())
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

}
