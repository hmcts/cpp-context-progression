package uk.gov.moj.cpp.progression.command;

import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.progression.domain.helper.JsonHelper.addProperty;
import static uk.gov.moj.cpp.progression.domain.helper.JsonHelper.removeProperty;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.command.api.UserDetailsLoader;
import uk.gov.moj.cpp.progression.command.service.DefenceQueryService;
import uk.gov.moj.cpp.progression.command.service.ProsecutionCaseQueryService;
import uk.gov.moj.cpp.progression.command.service.UserGroupQueryService;
import uk.gov.moj.cpp.progression.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class AddCourtDocumentApi {

    private static final String IS_CPS_CASE = "isCpsCase";
    private static final String IS_UNBUNDLED_DOCUMENT = "isUnbundledDocument";
    public static final String COURT_DOCUMENT = "courtDocument";
    public static final String DOCUMENT_TYPE_ID = "documentTypeId";
    private static final String NON_CPS_PROSECUTORS = "Non CPS Prosecutors";
    private static final String ORGANISATION_MIS_MATCH = "OrganisationMisMatch";

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private DefenceQueryService defenceQueryService;

    @Inject
    private UserGroupQueryService userGroupQueryService;

    @Inject
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Inject
    private Sender sender;

    @Inject
    private UserDetailsLoader userDetailsLoader;

    @Inject
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Handles("progression.add-court-document")
    public void handleAddCourtDocument(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(prepareMetadataForAddCourtDocument(envelope), preparePayloadForAddCourtDocument(envelope)));
    }

    @SuppressWarnings("squid:S3655")
    @Handles("progression.add-court-document-for-prosecutor")
    public void handleAddCourtDocumentForProsecutor(final JsonEnvelope envelope) {
        final Optional<DocumentTypeAccessReferenceData> documentTypeAccessReferenceData = referenceDataService.getDocumentTypeAccessReferenceData(requester, getDocumentTypeId(envelope));
        final Optional<UUID> caseId = getCaseId(envelope.payloadAsJsonObject());
        final UUID userId = envelope.metadata().userId().isPresent() ? fromString(envelope.metadata().userId().get()) : null;

        if (caseId.isPresent()) {
            final Optional<JsonObject> prosecutionCase = prosecutionCaseQueryService.getProsecutionCase(envelope, caseId.get());
            checkForbiddenRequest(envelope, caseId, userId, prosecutionCase);
        }

        if (documentTypeAccessReferenceData.isPresent() && toBoolean(documentTypeAccessReferenceData.get().getDefenceOnly())) {
            throw new ForbiddenRequestException("User is not authorised to use this section type!");
        }

        sender.send(envelopeFrom(prepareMetadataForAddCourtDocument(envelope), preparePayloadForAddCourtDocument(envelope)));
    }

    @SuppressWarnings("squid:S3655")
    private void checkForbiddenRequest(final JsonEnvelope envelope, final Optional<UUID> caseId, final UUID userId, final Optional<JsonObject> prosecutionCase) {
        if (prosecutionCase.isPresent()) {
            final ProsecutionCase prosecutionCaseObj = jsonObjectToObjectConverter.convert(prosecutionCase.get().getJsonObject("prosecutionCase"), ProsecutionCase.class);
            final Optional<String> orgMatch = userGroupQueryService.validateNonCPSUserOrg(envelope, userId, NON_CPS_PROSECUTORS, getShortName(prosecutionCaseObj));
            if (orgMatch.isPresent()) {
                if (ORGANISATION_MIS_MATCH.equals(orgMatch.get())) {
                    throw new ForbiddenRequestException("Forbidden!! Non CPS Prosecutor user cannot view court documents if it is not belongs to the same Prosecuting Authority of the user logged in");
                }
            } else if (!defenceQueryService.isUserProsecutingCase(envelope, caseId.get())) {
                throw new ForbiddenRequestException("Forbidden!! Cannot view court documents, user not prosecuting the case");
            }
        }
    }

    private String getShortName(final ProsecutionCase prosecutionCaseObj) {
        return nonNull(prosecutionCaseObj.getProsecutor()) && nonNull(prosecutionCaseObj.getProsecutor().getProsecutorCode()) ?
                prosecutionCaseObj.getProsecutor().getProsecutorCode() :
                prosecutionCaseObj.getProsecutionCaseIdentifier().getProsecutionAuthorityCode();
    }

    private Optional<UUID> getCaseId(final JsonObject payloadAsJsonObject) {
        final AddCourtDocument addCourtDocument = jsonObjectToObjectConverter.convert(payloadAsJsonObject, AddCourtDocument.class);
        if (nonNull(addCourtDocument.getCourtDocument().getDocumentCategory().getApplicationDocument())) {
            return of(addCourtDocument.getCourtDocument().getDocumentCategory().getApplicationDocument().getProsecutionCaseId());
        } else if (nonNull(addCourtDocument.getCourtDocument().getDocumentCategory().getDefendantDocument())) {
            return of(addCourtDocument.getCourtDocument().getDocumentCategory().getDefendantDocument().getProsecutionCaseId());
        } else if (nonNull(addCourtDocument.getCourtDocument().getDocumentCategory().getCaseDocument())) {
            return of(addCourtDocument.getCourtDocument().getDocumentCategory().getCaseDocument().getProsecutionCaseId());
        }
        return Optional.empty();
    }

    private UUID getDocumentTypeId(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonObject courtDocument = (JsonObject) payload.get(COURT_DOCUMENT);
        return fromString(courtDocument.getString(DOCUMENT_TYPE_ID));
    }

    private Metadata prepareMetadataForAddCourtDocument(final JsonEnvelope envelope) {
        return metadataFrom(envelope.metadata())
                .withName("progression.command.add-court-document")
                .build();
    }

    private JsonObject preparePayloadForAddCourtDocument(final JsonEnvelope envelope) {
        JsonObject payload = envelope.payloadAsJsonObject();
        JsonObject courtDocument = (JsonObject) payload.get(COURT_DOCUMENT);
        if (nonNull(courtDocument.get(IS_CPS_CASE))) {
            final boolean isCpsCase = courtDocument.getBoolean(IS_CPS_CASE);
            courtDocument = removeProperty(courtDocument, IS_CPS_CASE);
            payload = addProperty(payload, IS_CPS_CASE, isCpsCase);
        }
        if (nonNull(courtDocument.get(IS_UNBUNDLED_DOCUMENT))) {
            final boolean isUnbundledDocument = courtDocument.getBoolean(IS_UNBUNDLED_DOCUMENT);
            courtDocument = removeProperty(courtDocument, IS_UNBUNDLED_DOCUMENT);
            payload = addProperty(payload, IS_UNBUNDLED_DOCUMENT, isUnbundledDocument);
        }
        payload = addProperty(payload, COURT_DOCUMENT, courtDocument);
        return payload;
    }

    @Handles("progression.add-court-document-v2")
    public void handleV2(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("progression.command.add-court-document-v2")
                .build();
        sender.send(envelopeFrom(metadata, payload));
    }

    @Handles("progression.add-court-document-for-defence")
    public void handleAddCourtDocumentForDefence(final JsonEnvelope envelope) {

        final String defendantId = envelope.payloadAsJsonObject().getString("defendantId");
        final AddCourtDocument addCourtDocument = jsonObjectToObjectConverter.convert(envelope.asJsonObject(), AddCourtDocument.class);
        final DefendantDocument defendantDocument = addCourtDocument.getCourtDocument().getDocumentCategory().getDefendantDocument();
        if (defendantDocument != null) {
            if (defendantDocument.getDefendants().size() != 1) {
                throw new BadRequestException("Defendant in defendant Category must be only one");
            }
            if (!defendantDocument.getDefendants().get(0).equals(fromString(defendantId))) {
                throw new BadRequestException("Defendant in the Path and body are different");
            }
        }
        if (!userDetailsLoader.isPermitted(envelope, requester)) {
            throw new ForbiddenRequestException("User has neither associated or granted permission to upload");
        }

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("progression.command.add-court-document")
                .build();
        sender.send(envelopeFrom(metadata, removeProperty(envelope.payloadAsJsonObject(), "defendantId")));

    }
}
