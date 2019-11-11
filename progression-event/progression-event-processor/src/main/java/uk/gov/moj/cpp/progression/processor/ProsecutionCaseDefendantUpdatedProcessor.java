package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Objects;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3457", "squid:S3655",})
@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionCaseDefendantUpdatedProcessor {

    protected static final String PUBLIC_CASE_DEFENDANT_CHANGED = "public.progression.case-defendant-changed";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseDefendantUpdatedProcessor.class.getCanonicalName());

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("progression.event.prosecution-case-defendant-updated")
    public void handleProsecutionCaseDefendantUpdatedEvent(final JsonEnvelope jsonEnvelope) {
        final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ProsecutionCaseDefendantUpdated.class);
        final DefendantUpdate defendant = prosecutionCaseDefendantUpdated.getDefendant();
        LOGGER.debug("Received prosecution case defendant updated for caseId: " + defendant.getProsecutionCaseId());

        final JsonObject publicEventPayload = Json.createObjectBuilder()
                .add("defendant", objectToJsonObjectConverter.convert(updateDefendant(defendant))).build();
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_CASE_DEFENDANT_CHANGED).apply(publicEventPayload));
    }

    private DefendantUpdate updateDefendant(final DefendantUpdate defendant) {
        return DefendantUpdate.defendantUpdate().withId(defendant.getId())
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                .withProsecutionAuthorityReference(defendant.getProsecutionAuthorityReference())
                .withWitnessStatement(defendant.getWitnessStatement())
                .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                .withMitigation(defendant.getMitigation())
                .withMitigationWelsh(defendant.getMitigationWelsh())
                .withAssociatedPersons(defendant.getAssociatedPersons())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withPersonDefendant(defendant.getPersonDefendant())
                .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                .withPncId(defendant.getPncId())
                .withAliases(defendant.getAliases())
                .withIsYouth(defendant.getIsYouth())
                .build();
    }

}
