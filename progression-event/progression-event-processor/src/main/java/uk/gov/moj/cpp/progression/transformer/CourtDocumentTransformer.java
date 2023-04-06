package uk.gov.moj.cpp.progression.transformer;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.justice.core.courts.AddMaterialV2.addMaterialV2;
import static uk.gov.justice.core.courts.CourtApplicationSubject.courtApplicationSubject;
import static uk.gov.justice.core.courts.DefendantSubject.defendantSubject;
import static uk.gov.justice.core.courts.EventNotification.eventNotification;
import static uk.gov.justice.core.courts.ProsecutionCaseSubject.prosecutionCaseSubject;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.AddMaterialV2;
import uk.gov.justice.core.courts.CourtApplicationSubject;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantSubject;
import uk.gov.justice.core.courts.EventNotification;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCaseSubject;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.MaterialService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CourtDocumentTransformer {

    public static final String OUCODE = "oucode";
    private static final String BUSINESS_EVENT_TYPE = "defence-requested-to-notify-cps-of-material";
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtDocumentTransformer.class);

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    StringToJsonObjectConverter jsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private RefDataService referenceDataService;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private MaterialService materialService;

    public Optional<String> transform(final CourtDocument courtDocument, final Optional<JsonObject> prosecutionCaseJsonOptional, final JsonEnvelope envelope, final String notificationType) {

        final List<UUID> subjectBusinessObjectId = getSubjectBusinessObjectIdFromCaseOrApplicationOrDefendants(courtDocument);
        if (subjectBusinessObjectId.isEmpty()) {
            LOGGER.error("Unable to transform the payload. subjectBusinessObjectId is null. CourtDocumentId is {}", courtDocument.getCourtDocumentId());
            return empty();
        }

        final AddMaterialV2.Builder addMaterialV2 = buildAddMaterialV2(envelope, courtDocument);
        final CourtApplicationSubject courtApplicationSubject = buildCourtApplicationSubject(courtDocument);
        addMaterialV2.withCourtApplicationSubject(courtApplicationSubject);

        final Optional<ProsecutionCase> prosecutionCaseOptional = getProsecutionCase(prosecutionCaseJsonOptional);
        List<DefendantSubject> additionalDefendantSubject = new ArrayList<>();
        if (prosecutionCaseOptional.isPresent()) {
            final ProsecutionCaseSubject.Builder prosecutionCaseSubjectBuilder = prosecutionCaseSubject();
            final List<Defendant> defendantLinkedToDocument = prosecutionCaseOptional.get().getDefendants().stream().filter(defendant -> subjectBusinessObjectId.contains(defendant.getId())).collect(Collectors.toList());

            if (defendantLinkedToDocument.size()==1) {
                final DefendantSubject defendantSubject = buildDefendantSubject(defendantLinkedToDocument.get(0));
                prosecutionCaseSubjectBuilder.withDefendantSubject(defendantSubject);
            }else{
                additionalDefendantSubject= defendantLinkedToDocument.stream().map(v->buildDefendantSubject(v)).collect(Collectors.toList());
            }

            buildProsecutionCaseSubject(prosecutionCaseSubjectBuilder, prosecutionCaseOptional.get(), envelope);
            addMaterialV2.withProsecutionCaseSubject(prosecutionCaseSubjectBuilder.build());

        }

        final EventNotification eventNotification = buildEventNotification(addMaterialV2, subjectBusinessObjectId,notificationType,additionalDefendantSubject);

        return ofNullable(objectToJsonObjectConverter.convert(eventNotification).toString());
    }

    private AddMaterialV2.Builder buildAddMaterialV2(final JsonEnvelope envelope, final CourtDocument courtDocument) {
        final AddMaterialV2.Builder addMaterialV2 = addMaterialV2();
        ofNullable(courtDocument.getMaterials().get(0).getId()).ifPresent(addMaterialV2::withMaterial);
        ofNullable(courtDocument.getDocumentTypeDescription()).ifPresent(addMaterialV2::withMaterialType);
        ofNullable(courtDocument.getMimeType()).ifPresent(addMaterialV2::withMaterialContentType);
        ofNullable(courtDocument.getName()).ifPresent(addMaterialV2::withMaterialName);
        ofNullable(getMaterialFileName(envelope, courtDocument.getMaterials().get(0).getId())).ifPresent(addMaterialV2::withFileName);
        ofNullable(courtDocument.getNotificationType()).ifPresent(addMaterialV2::withNotificationType);
        return addMaterialV2;
    }

    private String getMaterialFileName(final JsonEnvelope envelope, final UUID materialId) {
        return materialService.getMaterialMetadataV2(envelope, materialId);
    }

    private EventNotification buildEventNotification(final AddMaterialV2.Builder addMaterialV2, final List<UUID> subjectBusinessObjectId, final String notificationType, final List<DefendantSubject> additionalDefendantSubject) {
        final EventNotification.Builder eventNotificationBuilder = eventNotification();
        eventNotificationBuilder.withSubjectBusinessObjectId(subjectBusinessObjectId.get(0));
        eventNotificationBuilder.withSubjectDetails(addMaterialV2.build());
        eventNotificationBuilder.withBusinessEventType(BUSINESS_EVENT_TYPE);
        eventNotificationBuilder.withAdditionalProperty("notificationType", Optional.ofNullable(notificationType).orElse("defence-disclosure"));
        eventNotificationBuilder.withAdditionalProperty("additionalDefendantSubject",additionalDefendantSubject);
        return eventNotificationBuilder.build();
    }

    private CourtApplicationSubject buildCourtApplicationSubject(final CourtDocument courtDocument) {
        final CourtApplicationSubject.Builder courtApplicationSubjectBuilder = courtApplicationSubject();
        if (ofNullable(courtDocument.getDocumentCategory().getApplicationDocument()).isPresent()) {
            return courtApplicationSubjectBuilder.withCourtApplicationId(courtDocument.getDocumentCategory().getApplicationDocument().getApplicationId()).build();
        }
        return null;
    }

    private void buildProsecutionCaseSubject(final ProsecutionCaseSubject.Builder prosecutionCaseSubjectBuilder, final ProsecutionCase prosecutionCase, final JsonEnvelope envelope) {

        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
        if (nonNull(prosecutionCaseIdentifier.getCaseURN())) {
            prosecutionCaseSubjectBuilder.withCaseUrn(prosecutionCaseIdentifier.getCaseURN());
        } else {
            prosecutionCaseSubjectBuilder.withCaseUrn(prosecutionCaseIdentifier.getProsecutionAuthorityReference());
        }

        if (nonNull(prosecutionCaseIdentifier.getProsecutionAuthorityOUCode())) {
            prosecutionCaseSubjectBuilder.withProsecutingAuthority(prosecutionCaseIdentifier.getProsecutionAuthorityOUCode());
        } else {
            prosecutionCaseSubjectBuilder.withProsecutingAuthority(callRefDataToGetProsecutionAuthority(prosecutionCaseIdentifier.getProsecutionAuthorityId(), envelope));
        }

    }


    private DefendantSubject buildDefendantSubject(final Defendant defendant) {
        final DefendantSubject.Builder defendantSubjectBuilder = defendantSubject();

        if (nonNull(defendant.getProsecutionAuthorityReference())) {
            defendantSubjectBuilder.withProsecutorDefendantId(defendant.getProsecutionAuthorityReference());
        }

        if (nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getArrestSummonsNumber())) {
            defendantSubjectBuilder.withAsn(defendant.getPersonDefendant().getArrestSummonsNumber());
        }

        if(nonNull(defendant.getCpsDefendantId())){
            defendantSubjectBuilder.withCpsDefendantId(defendant.getCpsDefendantId());
        }
        return defendantSubjectBuilder.build();
    }

    private Optional<ProsecutionCase> getProsecutionCase(final Optional<JsonObject> prosecutionCaseOptional) {
        return prosecutionCaseOptional.map(jsonObject -> jsonObjectToObjectConverter.
                convert(jsonObject.getJsonObject("prosecutionCase"),
                        ProsecutionCase.class));
    }

    private String callRefDataToGetProsecutionAuthority(final UUID prosecutionAuthorityId, final JsonEnvelope envelope) {
        final Optional<JsonObject> prosecutor = referenceDataService.getProsecutor(envelope, prosecutionAuthorityId, requester);
        if (prosecutor.isPresent()) {
            return prosecutor.get().getString(OUCODE);
        }
        return EMPTY;
    }

    private List<UUID> getSubjectBusinessObjectIdFromCaseOrApplicationOrDefendants(final CourtDocument courtDocument) {

        if (nonNull(courtDocument.getDocumentCategory().getCaseDocument())) {
            return Arrays.asList(courtDocument.getDocumentCategory().getCaseDocument().getProsecutionCaseId());
        } else if (nonNull(courtDocument.getDocumentCategory().getApplicationDocument())) {
            return Arrays.asList(courtDocument.getDocumentCategory().getApplicationDocument().getApplicationId());
        } else if (nonNull(courtDocument.getDocumentCategory().getDefendantDocument())) {
            return courtDocument.getDocumentCategory().getDefendantDocument().getDefendants();
        }
        return newArrayList();
    }
}