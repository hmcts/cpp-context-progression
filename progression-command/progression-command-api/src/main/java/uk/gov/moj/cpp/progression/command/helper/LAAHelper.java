package uk.gov.moj.cpp.progression.command.helper;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.service.ProsecutionCaseQueryService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.inject.Inject;
import javax.json.JsonObject;

public class LAAHelper {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ProsecutionCaseQueryService progressionQueryService;

    @Inject
    private CourtApplicationRepository courtApplicationRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    public void validateInputsForApplication(final String stringApplicationId, final JsonEnvelope envelope) {

        if (isNull(stringApplicationId) || isInvalidUUID(stringApplicationId)) {
            throw new BadRequestException("applicationId is not a valid UUID!");
        }
        final UUID applicationId = fromString(stringApplicationId);

        final CourtApplication courtApplication = getCourtApplication(envelope, applicationId);

        if (nonNull(courtApplication.getParentApplicationId())) {
            throw new BadRequestException("Parent Application found for application id: " + applicationId);
        }
        if (nonNull(courtApplication.getCourtApplicationCases())) {
            boolean isOffencesAvailableInApplication = courtApplication.getCourtApplicationCases().stream()
                    .anyMatch(courtApplicationCase -> nonNull(courtApplicationCase.getOffences()));
            if (isOffencesAvailableInApplication) {
                throw new BadRequestException("Offences found for application id: " + applicationId);
            }
        }
    }

    public CourtApplication getCourtApplication(JsonEnvelope envelope, UUID applicationId) {
        final JsonObject courtApplicationJsonObject = progressionQueryService.getCourtApplicationById(applicationId, envelope)
                .orElseThrow(() -> new BadRequestException("Application not found for id: " + applicationId));
        return jsonObjectToObjectConverter.convert(courtApplicationJsonObject.getJsonObject("courtApplication"), CourtApplication.class);
    }

    public static boolean isInvalidUUID(final String string) {
        try {
            fromString(string);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public List<UUID> getChildApplicationList(final UUID applicationId) {
        final List<CourtApplicationEntity> childApplications = courtApplicationRepository.findByParentApplicationId(applicationId);

        if (isEmpty(childApplications)) {
            return emptyList();
        }

        return childApplications.stream()
                .map(CourtApplicationEntity::getPayload)
                .map(stringToJsonObjectConverter::convert)
                .map(json -> json.getString("id", null))
                .filter(Objects::nonNull)
                .map(UUID::fromString)
                .toList();

    }
}
