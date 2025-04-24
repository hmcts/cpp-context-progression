package uk.gov.moj.cpp.progression.query;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.getUUID;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.SharedCourtDocumentsLinksForApplication;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.view.service.ReferenceDataService;
import uk.gov.moj.cpp.progression.query.view.service.SharedAllCourtDocumentsService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SharedCourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CpsSendNotificationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SharedCourtDocumentRepository;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;

@ServiceComponent(Component.QUERY_VIEW)
public class SharedCourtDocumentsQueryView {

    private static final int BATCH_SIZE = 50;
    public static final String PROGRESSION_QUERY_SHARED_COURT_DOCUMENTS = "progression.query.shared-court-documents";
    public static final String PROGRESSION_QUERY_SHARED_COURT_DOCUMENTS_LINKS_FOR_APPLICATION = "progression.query.application.shared-court-documents-links";
    private static final String APPLICATION_ID = "applicationId";
    private static final String HEARING_ID = "hearingId";
    private static final String TRIAL_TYPE_FLAG = "trialTypeFlag";
    private static final String ID = "id";

    @Inject
    private SharedCourtDocumentRepository sharedCourtDocumentRepository;

    @Inject
    private CourtDocumentRepository courtDocumentRepository;

    @Inject
    private SharedAllCourtDocumentsService sharedAllCourtDocumentsService;

    @Inject
    private CourtApplicationCaseRepository courtApplicationCaseRepository;

    @Inject
    private HearingApplicationRepository hearingApplicationRepository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CourtDocumentTransform courtDocumentTransform;

    @Inject
    private CpsSendNotificationRepository cpsSendNotificationRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ListToJsonArrayConverter<SharedCourtDocumentsLinksForApplication> sharedCourtDocumentsLinksForApplicationListToJsonArrayConverter;

    @Handles(PROGRESSION_QUERY_SHARED_COURT_DOCUMENTS)
    public JsonEnvelope getSharedCourtDocuments(final JsonEnvelope envelope) {

        if (!envelope.payloadAsJsonObject().containsKey("caseId") || !envelope.payloadAsJsonObject().containsKey("defendantId")) {
            throw new BadRequestException(String.format("%s no search parameter specified ", PROGRESSION_QUERY_SHARED_COURT_DOCUMENTS));
        }
        final UUID hearingId = fromString(envelope.payloadAsJsonObject().getString(HEARING_ID));
        final UUID userGroupId = fromString(envelope.payloadAsJsonObject().getString("userGroupId"));
        final UUID caseId = fromString(envelope.payloadAsJsonObject().getString("caseId"));
        final UUID defendantId = fromString(envelope.payloadAsJsonObject().getString("defendantId"));

        final List<SharedCourtDocumentEntity> sharedDocuments = sharedCourtDocumentRepository.findByHearingIdAndDefendantIdForSelectedCaseForUserGroup(caseId, hearingId, userGroupId, defendantId);
        final List<CourtDocument> courtDocuments = getCourtDocumentsByBatch(sharedDocuments);

        final List<CourtDocumentIndex> courtDocumentIndices = courtDocuments.stream()
                .map(courtDocumentFiltered -> courtDocumentTransform.transform(courtDocumentFiltered, cpsSendNotificationRepository).build()).sorted((o1, o2) -> {
                    if (CollectionUtils.isNotEmpty(o1.getDocument().getMaterials()) &&
                            CollectionUtils.isNotEmpty(o2.getDocument().getMaterials()) &&
                            nonNull(o2.getDocument().getMaterials().get(0).getUploadDateTime()) &&
                            nonNull(o1.getDocument().getMaterials().get(0).getUploadDateTime())) {
                        return o2.getDocument().getMaterials().get(0).getUploadDateTime().compareTo(o1.getDocument().getMaterials().get(0).getUploadDateTime());
                    }
                    return -1;
                }).filter(courtDocumentIndex -> courtDocumentIndex.getDefendantIds().isEmpty() || courtDocumentIndex.getDefendantIds().contains(defendantId)).collect(toList());

        final CourtDocumentsSearchResult result = new CourtDocumentsSearchResult();
        result.setDocumentIndices(courtDocumentIndices);

        final JsonObject resultJson = objectToJsonObjectConverter.convert(result);
        return envelopeFrom(envelope.metadata(), resultJson);
    }

    @Handles(PROGRESSION_QUERY_SHARED_COURT_DOCUMENTS_LINKS_FOR_APPLICATION)
    public JsonEnvelope getApplicationSharedCourtDocumentsLinks(final JsonEnvelope envelope) {
        final List<SharedCourtDocumentsLinksForApplication> sharedCourtDocumentsLinks = new ArrayList<>();
        final UUID applicationId = getUUID(envelope.payloadAsJsonObject(), APPLICATION_ID).orElseThrow(() -> new IllegalStateException("No APPLICATION_ID Supplied"));
        final UUID applicationHearingId = getUUID(envelope.payloadAsJsonObject(), HEARING_ID).orElseThrow(() -> new IllegalStateException("No HEARING_ID Supplied"));

        final List<CourtApplicationCaseEntity> courtApplicationCaseEntities = courtApplicationCaseRepository.findByApplicationId(applicationId);

        courtApplicationCaseEntities.forEach(courtApplicationCaseEntity -> {
            final UUID caseId = courtApplicationCaseEntity.getId().getCaseId();
            final String caseUrn = courtApplicationCaseEntity.getCaseReference();
            final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId);
            final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload()), ProsecutionCase.class);
            final List<Defendant> defendants = prosecutionCase.getDefendants();

            if ("INACTIVE".equals(prosecutionCase.getCaseStatus())) {
                final Hearing hearing = getHearing(applicationId, applicationHearingId);
                final boolean isTrialHearing = isTrialHearing(envelope, hearing);
                if (isTrialHearing) {
                    sharedCourtDocumentsLinks.addAll(sharedAllCourtDocumentsService.getSharedAllCourtDocumentsForTrialHearing(envelope, caseId, caseUrn, defendants, applicationHearingId));

                } else {
                    sharedCourtDocumentsLinks.addAll(sharedAllCourtDocumentsService.getSharedAllCourtDocuments(caseId, caseUrn, defendants));
                }
            }
        });

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                Json.createObjectBuilder().add("sharedCourtDocumentsLinksForApplication", sharedCourtDocumentsLinksForApplicationListToJsonArrayConverter.convert(sharedCourtDocumentsLinks)).build());

    }

    private boolean isTrialHearing(final JsonEnvelope envelope, final Hearing hearing) {
        final JsonArray hearingTypes = referenceDataService.getHearingTypes(envelope);
        return  hearingTypes.stream()
                .map(JsonObject.class::cast)
                .anyMatch(jsonObject -> hearing.getType().getId().equals(fromString(jsonObject.getString(ID)))
                        && jsonObject.getBoolean(TRIAL_TYPE_FLAG));
    }

    private Hearing getHearing(final UUID applicationId, final UUID hearingId) {
        final HearingApplicationEntity entity = hearingApplicationRepository.findBy(new HearingApplicationKey(applicationId, hearingId));
        return jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(entity.getHearing().getPayload()), Hearing.class);
    }

    private List<CourtDocument> getCourtDocumentsByBatch(final List<SharedCourtDocumentEntity> sharedDocuments) {
        final List<CourtDocument> courtDocumentsResult = new ArrayList<>();
        for (int i = 0; i < sharedDocuments.size(); i += BATCH_SIZE) {
            final List<UUID> courtDocumentIds = sharedDocuments.stream().skip(i).limit(BATCH_SIZE).map(SharedCourtDocumentEntity::getCourtDocumentId).collect(toList());
            final List<CourtDocumentEntity> courtDocumentEntities = courtDocumentRepository.findByCourtDocumentIdsAndAreNotRemoved(courtDocumentIds);
            final List<CourtDocument> courtDocuments = courtDocumentEntities.stream().map(this::courtDocument).collect(toList());
            courtDocumentsResult.addAll(courtDocuments);
        }
        return courtDocumentsResult;
    }

    private CourtDocument courtDocument(final CourtDocumentEntity courtDocumentEntity) {
        return jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(courtDocumentEntity.getPayload()), CourtDocument.class);
    }

}
