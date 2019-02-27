package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.ConvictionDateAdded;
import uk.gov.justice.core.courts.ConvictionDateRemoved;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@SuppressWarnings({"squid:S3655", "squid:S1602"})
@ServiceComponent(EVENT_LISTENER)
public class ConvictionDateEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProsecutionCaseRepository repository;

    @Handles("progression.event.conviction-date-added")
    public void addConvictionDate(final JsonEnvelope event) {
        final ConvictionDateAdded convictionDateAdded = jsonObjectConverter.convert(event.payloadAsJsonObject(), ConvictionDateAdded.class);
        ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(convictionDateAdded.getCaseId());
        final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
        ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        UUID offenceIdToBeUpdated = convictionDateAdded.getOffenceId();

        for (Defendant defendant : prosecutionCase.getDefendants()) {
            Offence updatedOffence = null;
            boolean isConvictionDateUpdated = false;
            for (Offence offence : defendant.getOffences()) {
                if (offence.getId().equals(offenceIdToBeUpdated)) {
                    isConvictionDateUpdated = true;
                    updatedOffence = updateOffence(offence, convictionDateAdded.getConvictionDate());
                }
            }
            updateOffenceWithConvictionDate(offenceIdToBeUpdated, defendant, updatedOffence, isConvictionDateUpdated);
        }
        repository.save(getProsecutionCaseEntity(prosecutionCase));
    }

    @Handles("progression.event.conviction-date-removed")
    public void removeConvictionDate(final JsonEnvelope event) {
        final ConvictionDateRemoved convictionDateRemoved = jsonObjectConverter.convert(event.payloadAsJsonObject(), ConvictionDateRemoved.class);
        ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(convictionDateRemoved.getCaseId());
        final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
        ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        UUID offenceIdToBeUpdated = convictionDateRemoved.getOffenceId();

        for (Defendant defendant : prosecutionCase.getDefendants()) {
            Offence updatedOffence = null;
            boolean isConvictionDateUpdated = false;
            for (Offence offence : defendant.getOffences()) {
                if (offence.getId().equals(offenceIdToBeUpdated)) {
                    isConvictionDateUpdated = true;
                    updatedOffence = updateOffence(offence, null);
                }
            }
            updateOffenceWithConvictionDate(offenceIdToBeUpdated, defendant, updatedOffence, isConvictionDateUpdated);
        }
        repository.save(getProsecutionCaseEntity(prosecutionCase));
    }

    private Offence updateOffence(Offence offence, String convictionDate) {
        return Offence.offence()
                .withArrestDate(offence.getArrestDate())
                .withChargeDate(offence.getChargeDate())
                .withConvictionDate(convictionDate)
                .withCount(offence.getCount())
                .withDateOfInformation(offence.getDateOfInformation())
                .withEndDate(offence.getEndDate())
                .withId(offence.getId())
                .withIndicatedPlea(offence.getIndicatedPlea())
                .withModeOfTrial(offence.getModeOfTrial())
                .withNotifiedPlea(offence.getNotifiedPlea())
                .withOffenceCode(offence.getOffenceCode())
                .withOffenceDefinitionId(offence.getOffenceDefinitionId())
                .withOffenceFacts(offence.getOffenceFacts())
                .withOffenceLegislation(offence.getOffenceLegislation())
                .withOffenceLegislationWelsh(offence.getOffenceLegislationWelsh())
                .withOffenceTitle(offence.getOffenceTitle())
                .withOffenceTitleWelsh(offence.getOffenceTitleWelsh())
                .withOrderIndex(offence.getOrderIndex())
                .withPlea(offence.getPlea())
                .withStartDate(offence.getStartDate())
                .withVerdict(offence.getVerdict())
                .withWording(offence.getWording())
                .withWordingWelsh(offence.getWordingWelsh())
                .build();
    }

    private void updateOffenceWithConvictionDate(UUID offenceIdToBeUpdated, Defendant defendant, Offence updatedOffence, boolean isConvictionDateUpdated) {
        if (isConvictionDateUpdated) {
            List<Offence> testOffences = defendant.getOffences().stream()
                    .filter(offence -> !offence.getId().equals(offenceIdToBeUpdated))
                    .collect(Collectors.toList());
            defendant.getOffences().clear();
            defendant.getOffences().addAll(testOffences);
            defendant.getOffences().add(updatedOffence);
        }
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {
        JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }
}
