package uk.gov.moj.cpp.prosecutioncase.event.listener;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantCaseOffences;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseOffencesUpdated;
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
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@SuppressWarnings("squid:S3655")
@ServiceComponent(EVENT_LISTENER)
public class ProsecutionCaseOffencesUpdatedEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProsecutionCaseRepository repository;

    @Handles("progression.event.prosecution-case-offences-updated")
    public void processProsecutionCaseOffencesUpdated(final JsonEnvelope event) {
        final ProsecutionCaseOffencesUpdated prosecutionCaseOffencesUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseOffencesUpdated.class);
        final DefendantCaseOffences defendantCaseOffences = prosecutionCaseOffencesUpdated.getDefendantCaseOffences();
        if (defendantCaseOffences != null) {

            final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(defendantCaseOffences.getProsecutionCaseId());
            final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
            final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
            final Optional<Defendant> defendant = prosecutionCase.getDefendants().stream()
                    .filter(d -> d.getId().equals(defendantCaseOffences.getDefendantId()))
                    .findFirst();
            if (defendant.isPresent() && !defendantCaseOffences.getOffences().isEmpty()) {
                final List<Offence> persistedOffences = defendant.get().getOffences().stream().collect(Collectors.toList());

                //Delete
                final List<Offence> offenceDetailListDel = getDeletedOffences(defendantCaseOffences.getOffences(), defendant.get().getOffences());
                defendant.get().getOffences().removeAll(offenceDetailListDel);

                //amend
                mergeOffence(persistedOffences, defendantCaseOffences.getOffences(), defendant.get().getOffences());

                //Add
                final List<Offence> offenceDetailListAdd = getAddedOffences(defendantCaseOffences.getOffences(), defendant.get().getOffences());
                defendant.get().getOffences().addAll(offenceDetailListAdd);
            }
            repository.save(getProsecutionCaseEntity(prosecutionCase));
        }
    }

    private static List<Offence> getDeletedOffences(
            final List<Offence> commandOffences,
            final List<Offence> existingOffences) {
        return existingOffences.stream()
                .filter(existingOffence -> !commandOffences.stream()
                        .map(Offence::getId)
                        .collect(Collectors.toList())
                        .contains(existingOffence.getId()))
                .collect(Collectors.toList());
    }

    private static List<Offence> getAddedOffences(
            final List<Offence> commandOffences,
            final List<Offence> existingOffences) {
        return commandOffences.stream()
                .filter(commandOffence -> !existingOffences.stream()
                        .map(Offence::getId)
                        .collect(Collectors.toList())
                        .contains(commandOffence.getId()))
                .collect(Collectors.toList());
    }

    private Offence updateOffence(final Offence persistedOffence, final Offence updatedOffence) {
        return Offence.offence()
                .withId(persistedOffence.getId())
                .withOffenceCode(updatedOffence.getOffenceCode())
                .withStartDate(updatedOffence.getStartDate())
                .withArrestDate(updatedOffence.getArrestDate())
                .withChargeDate(updatedOffence.getChargeDate())
                .withConvictionDate(persistedOffence.getConvictionDate())
                .withEndDate(updatedOffence.getEndDate())
                .withIndicatedPlea(persistedOffence.getIndicatedPlea())
                .withPlea(persistedOffence.getPlea())
                .withOffenceTitle(updatedOffence.getOffenceTitle())
                .withOffenceTitleWelsh(updatedOffence.getOffenceTitleWelsh())
                .withWording(updatedOffence.getWording())
                .withWordingWelsh(updatedOffence.getWordingWelsh())
                .withOffenceLegislation(updatedOffence.getOffenceLegislation())
                .withOffenceLegislationWelsh(updatedOffence.getOffenceLegislationWelsh())
                .withOrderIndex(persistedOffence.getOrderIndex())
                .withOffenceFacts(persistedOffence.getOffenceFacts())
                .withOffenceDefinitionId(persistedOffence.getOffenceDefinitionId())
                .withNotifiedPlea(persistedOffence.getNotifiedPlea())
                .withModeOfTrial(persistedOffence.getModeOfTrial())
                .withCount(updatedOffence.getCount()).build();
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {

        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }

    private void mergeOffence(final List<Offence> persistedOffences, final List<Offence> updatedOffences, List<Offence> originalOffences) {
        persistedOffences.forEach(offencePersisted ->
                updatedOffences.forEach(offenceDetail -> {
                    if (offencePersisted.getId().equals(offenceDetail.getId())) {
                        final Offence updatedOffence = updateOffence(offencePersisted, offenceDetail);
                        originalOffences.remove(offencePersisted);
                        originalOffences.add(updatedOffence);
                    }
                })
        );


    }
}
