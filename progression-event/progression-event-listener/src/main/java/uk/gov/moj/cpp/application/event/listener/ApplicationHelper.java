package uk.gov.moj.cpp.application.event.listener;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.core.courts.Offence.offence;

import uk.gov.justice.core.courts.ApplicationOffencesUpdated;
import uk.gov.justice.core.courts.ApplicationReporderOffencesUpdated;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;

public class ApplicationHelper {

    public static CourtApplication getPersistedCourtApplication(final CourtApplicationEntity applicationEntity, JsonObjectToObjectConverter jsonObjectToObjectConverter, StringToJsonObjectConverter stringToJsonObjectConverter) {
        final JsonObject applicationJson = stringToJsonObjectConverter.convert(applicationEntity.getPayload());
        return jsonObjectToObjectConverter.convert(applicationJson, CourtApplication.class);
    }

    public static List<UUID> getRelatedCaseIds(final UUID offenceId, final CourtApplication courtApplication) {

        if (isNull(courtApplication.getCourtApplicationCases())) {
            return emptyList();
        }

        return courtApplication.
                getCourtApplicationCases().
                stream().
                map(applicationCase -> {
                    if (isNull(applicationCase.getOffences())) {
                        return null;
                    }

                    boolean hasMatchingOffence = applicationCase.getOffences()
                            .stream()
                            .anyMatch(offence -> offenceId.equals(offence.getId()));

                    return hasMatchingOffence ? applicationCase.getProsecutionCaseId() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static void updateCase(List<UUID> caseIds,
                                  CourtApplication courtApplication,
                                  ApplicationOffencesUpdated applicationOffencesUpdated,
                                  ProsecutionCaseRepository prosecutionCaseRepository,
                                  JsonObjectToObjectConverter jsonObjectToObjectConverter,
                                  StringToJsonObjectConverter stringToJsonObjectConverter,
                                  ObjectToJsonObjectConverter objectToJsonObjectConverter) {
        caseIds
                .forEach(caseId -> {
                    var prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId);
                    final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
                    final ProsecutionCase persistentProsecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
                    final ProsecutionCase updateProsecutionCase = updateProsecutionCase(persistentProsecutionCase, courtApplication.getSubject().getMasterDefendant().getMasterDefendantId(), applicationOffencesUpdated.getOffenceId(), applicationOffencesUpdated.getLaaReference());
                    if (nonNull(updateProsecutionCase)) {
                        prosecutionCaseRepository.save(getProsecutionCaseEntity(updateProsecutionCase, objectToJsonObjectConverter));
                    }
                });
    }

    public static void updateCase(List<UUID> caseIds,
                                  CourtApplication courtApplication,
                                  ApplicationReporderOffencesUpdated applicationOffencesUpdated,
                                  ProsecutionCaseRepository prosecutionCaseRepository,
                                  JsonObjectToObjectConverter jsonObjectToObjectConverter,
                                  StringToJsonObjectConverter stringToJsonObjectConverter,
                                  ObjectToJsonObjectConverter objectToJsonObjectConverter) {
        caseIds
                .forEach(caseId -> {
                    var prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId);
                    final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
                    final ProsecutionCase persistentProsecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
                    final ProsecutionCase updateProsecutionCase = updateProsecutionCase(persistentProsecutionCase, courtApplication.getSubject().getMasterDefendant().getMasterDefendantId(), applicationOffencesUpdated.getOffenceId(), applicationOffencesUpdated.getLaaReference());
                    if (nonNull(updateProsecutionCase)) {
                        prosecutionCaseRepository.save(getProsecutionCaseEntity(updateProsecutionCase, objectToJsonObjectConverter));
                    }
                });
    }

    public static void updateCase(final List<UUID> caseIds,
                                  final CourtApplication courtApplication,
                                  final LaaReference laaReference,
                                  final ProsecutionCaseRepository prosecutionCaseRepository,
                                  final JsonObjectToObjectConverter jsonObjectToObjectConverter,
                                  final StringToJsonObjectConverter stringToJsonObjectConverter,
                                  final ObjectToJsonObjectConverter objectToJsonObjectConverter) {
        caseIds
                .forEach(caseId -> {
                    var prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId);
                    final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
                    final ProsecutionCase persistentProsecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
                    final ProsecutionCase updateProsecutionCase = updateProsecutionCase(persistentProsecutionCase, courtApplication.getSubject().getMasterDefendant().getMasterDefendantId(), laaReference);
                    if (nonNull(updateProsecutionCase)) {
                        prosecutionCaseRepository.save(getProsecutionCaseEntity(updateProsecutionCase, objectToJsonObjectConverter));
                    }
                });
    }


    private static ProsecutionCase updateProsecutionCase(final ProsecutionCase persistentProsecutionCase, final UUID masterDefendantId, final UUID offenceId, final LaaReference laaReference) {
        final List<Defendant> defendants = updateDefendants(persistentProsecutionCase, masterDefendantId, offenceId, laaReference);

        return ProsecutionCase.prosecutionCase()
                .withValuesFrom(persistentProsecutionCase)
                .withDefendants(defendants)
                .build();
    }

    private static ProsecutionCase updateProsecutionCase(final ProsecutionCase persistentProsecutionCase, final UUID masterDefendantId, final LaaReference laaReference) {
        final List<Defendant> defendants = updateDefendants(persistentProsecutionCase, masterDefendantId, laaReference);

        return ProsecutionCase.prosecutionCase()
                .withValuesFrom(persistentProsecutionCase)
                .withDefendants(defendants)
                .build();
    }

    private static List<Defendant> updateDefendants(final ProsecutionCase persistentProsecutionCase, final UUID masterDefendantId, final UUID offenceId, final LaaReference laaReference) {
        return persistentProsecutionCase
                .getDefendants()
                .stream()
                .map(defendant -> {
                    if (masterDefendantId.equals(defendant.getMasterDefendantId())) {
                        return updateDefendant(defendant, offenceId, laaReference);
                    } else {
                        return defendant;
                    }
                })
                .collect(Collectors.toList());
    }

    private static List<Defendant> updateDefendants(final ProsecutionCase persistentProsecutionCase, final UUID masterDefendantId, final LaaReference laaReference) {
        return persistentProsecutionCase
                .getDefendants()
                .stream()
                .map(defendant -> {
                    if (masterDefendantId.equals(defendant.getMasterDefendantId())) {
                        return updateDefendant(defendant, laaReference);
                    } else {
                        return defendant;
                    }
                }).toList();
    }

    private static Defendant updateDefendant(final Defendant defendant, final UUID offenceId, final LaaReference laaReference) {

        final List<Offence> offences = updateOffences(defendant, offenceId, laaReference);

        return Defendant.defendant()
                .withValuesFrom(defendant)
                .withLegalAidStatus(laaReference.getOffenceLevelStatus())
                .withOffences(offences)
                .build();

    }

    private static Defendant updateDefendant(final Defendant defendant, final LaaReference laaReference) {

        final List<Offence> offences = updateOffences(defendant, laaReference);

        return Defendant.defendant()
                .withValuesFrom(defendant)
                .withLegalAidStatus(laaReference.getOffenceLevelStatus())
                .withOffences(offences)
                .build();

    }

    private static List<Offence> updateOffences(final Defendant defendant, final UUID offenceId, final LaaReference laaReference) {
        return defendant.getOffences()
                .stream()
                .map(offence -> {
                    if (offenceId.equals(offence.getId())) {
                        return offence()
                                .withValuesFrom(offence)
                                .withLaaApplnReference(laaReference)
                                .build();
                    } else {
                        return offence;
                    }
                })
                .collect(Collectors.toList());
    }

    private static List<Offence> updateOffences(final Defendant defendant, final LaaReference laaReference) {
        return defendant.getOffences()
                .stream()
                .map(offence -> offence()
                        .withValuesFrom(offence)
                        .withLaaApplnReference(laaReference)
                        .build()
                ).toList();
    }


    private static ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase, ObjectToJsonObjectConverter objectToJsonObjectConverter) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        if (nonNull(prosecutionCase.getGroupId())) {
            pCaseEntity.setGroupId(prosecutionCase.getGroupId());
        }
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }

}
