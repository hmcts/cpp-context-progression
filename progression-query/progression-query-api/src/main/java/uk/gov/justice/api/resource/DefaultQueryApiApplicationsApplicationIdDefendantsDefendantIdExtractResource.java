package uk.gov.justice.api.resource;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.api.resource.utils.CourtExtractTransformer;
import uk.gov.justice.api.resource.utils.payload.PleaValueDescriptionBuilder;
import uk.gov.justice.api.resource.utils.payload.ResultTextFlagBuilder;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.exract.CourtExtractRequested;
import uk.gov.justice.services.adapter.rest.mapping.ActionMapper;
import uk.gov.justice.services.adapter.rest.processor.RestProcessor;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.api.exception.DocumentGeneratorException;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655"})
@Stateless
@Adapter(Component.QUERY_API)
public class DefaultQueryApiApplicationsApplicationIdDefendantsDefendantIdExtractResource implements QueryApiLinkedApplicationsExtractResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultQueryApiApplicationsApplicationIdDefendantsDefendantIdExtractResource.class);
    private static final String APPLICATION_AT_A_GLANCE_QUERY_NAME = "progression.query.application";
    public static final String LINKED_APPLICATION_EXTRACT_TEMPLATE = "CrownCourtExtract";
    private static final String SUBJECT = "subject";
    private static final String MASTER_DEFENDANT = "masterDefendant";
    private static final String DEFENDANT_CASE = "defendantCase";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String CASE_ID = "caseId";

    @Inject
    RestProcessor restProcessor;

    @Inject
    @Named("DefaultQueryApiApplicationsApplicationIdDefendantsDefendantIdExtractResourceActionMapper")
    ActionMapper actionMapper;

    @Inject
    InterceptorChainProcessor interceptorChainProcessor;

    @Inject
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @Inject
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CourtExtractTransformer courtExtractTransformer;
    @Inject
    private PleaValueDescriptionBuilder pleaValueDescriptionBuilder;

    @Inject
    private ResultTextFlagBuilder resultTextFlagBuilder;


    private UUID userId;

    @Override
    public Response getApplicationsByApplicationIdDefendantsByDefendantIdExtract(String applicationId, String defendantId, String hearingIds, UUID userId) {
        this.userId = userId;

        if (isNull(hearingIds) || hearingIds.isEmpty()) {
            throw new IllegalArgumentException("hearingIds query parameter cannot be empty or null for the template CrownCourtExtract");
        }
        final List<String> hearingIdList = asList(hearingIds.trim().split(","));

        final JsonEnvelope documentQuery = getApplicationByIdQuery(applicationId, userId);
        final JsonEnvelope document = interceptorChainProcessor.process(interceptorContextWithInput(documentQuery)).get();
        if (document.payloadIsNull() || document.payloadAsJsonObject().isEmpty()) {
            LOGGER.info(" ### No record found for the application id: {} ### ", applicationId);
            return null;
        }

        return getDocumentContent(document, hearingIdList, applicationId, defendantId);
    }


    private Response getDocumentContent(final JsonEnvelope document, final List<String> hearingIdList, final String applicationId, final String defendantId) {

        final UUID systemUser = serviceContextSystemUserProvider.getContextSystemUserId()
                .orElseThrow(() -> new WebApplicationException("System user for progression context not found"));
        final String pdfMimeType = "application/pdf";

        final JsonEnvelope linkedProsecutionCaseDocument = getLinkedProsecutionCase(document, applicationId, defendantId);

        if (nonNull(linkedProsecutionCaseDocument)) {
            final InputStream documentInputStream = getPdfDocument(linkedProsecutionCaseDocument, hearingIdList, defendantId, systemUser);
            final Response.ResponseBuilder responseBuilder = status(OK).entity(documentInputStream);
            return responseBuilder
                    .header(CONTENT_TYPE, pdfMimeType)
                    .build();
        }

        LOGGER.warn("### No linked case found! could not generate extract for applicationId: {} and defendantId: {} ### ", applicationId, defendantId);
        return null;
    }

    private JsonEnvelope getLinkedProsecutionCase(final JsonEnvelope document, final String applicationId, final String defendantId) {

        final JsonObject applicationJson = document.payloadAsJsonObject().getJsonObject("courtApplication");

        if (applicationJson.containsKey(SUBJECT)
                && applicationJson.getJsonObject(SUBJECT).containsKey(MASTER_DEFENDANT)
                && applicationJson.getJsonObject(SUBJECT).getJsonObject(MASTER_DEFENDANT).containsKey(DEFENDANT_CASE)
                && !applicationJson.getJsonObject(SUBJECT).getJsonObject(MASTER_DEFENDANT).getJsonArray(DEFENDANT_CASE).isEmpty()
                && applicationJson.getJsonObject(SUBJECT).getJsonObject(MASTER_DEFENDANT).getJsonArray(DEFENDANT_CASE).stream()
                .map(JsonValue::asJsonObject).anyMatch(dcJson -> defendantId.equals(dcJson.getString(DEFENDANT_ID)))
        ) {
            final Optional<String> caseId = applicationJson.getJsonObject(SUBJECT).getJsonObject(MASTER_DEFENDANT).getJsonArray(DEFENDANT_CASE).stream()
                    .map(JsonValue::asJsonObject)
                    .filter(dcJson -> defendantId.equals(dcJson.getString(DEFENDANT_ID)))
                    .map(dcJson -> dcJson.getString(CASE_ID))
                    .findFirst();

            if (caseId.isPresent()) {
                final JsonEnvelope prosecutionCaseQuery = getProsecutionCaseQuery(caseId.get(), userId);
                return interceptorChainProcessor.process(interceptorContextWithInput(prosecutionCaseQuery)).get();
            }
        }

        LOGGER.warn("### No Linked case found for applicationId: {} and defendantId: {} ### ", applicationId, defendantId);
        return null;
    }

    private InputStream getPdfDocument(final JsonEnvelope document, final List<String> hearingIdList, final String defendantId, final UUID systemUser) {
        final byte[] resultOrderAsByteArray;
        final InputStream documentInputStream;
        try {
            final JsonObject payload = transformToTemplateConvert(document.payloadAsJsonObject(), defendantId, LINKED_APPLICATION_EXTRACT_TEMPLATE, hearingIdList);
            LOGGER.info("create application extract with payload : {}", payload);
            JsonObject newPayload = resultTextFlagBuilder.rebuildWithResultTextFlag(pleaValueDescriptionBuilder.rebuildPleaWithDescription(payload));

            resultOrderAsByteArray = documentGeneratorClientProducer.documentGeneratorClient().generatePdfDocument(newPayload, LINKED_APPLICATION_EXTRACT_TEMPLATE, systemUser);
            documentInputStream = new ByteArrayInputStream(resultOrderAsByteArray);
        } catch (IOException e) {
            LOGGER.error("application extract generate Pdf document failed ", e);
            throw new DocumentGeneratorException();
        }
        return documentInputStream;
    }

    private JsonObject transformToTemplateConvert(JsonObject jsonObject, final String defendantId, final String extractType, final List<String> hearingIdList) {
        final GetHearingsAtAGlance hearingsAtAGlance = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("hearingsAtAGlance"), GetHearingsAtAGlance.class);
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("prosecutionCase"), ProsecutionCase.class);
        final CourtExtractRequested courtExtractRequested = courtExtractTransformer.getCourtExtractRequested(hearingsAtAGlance, defendantId, extractType, hearingIdList, userId, prosecutionCase);
        return objectToJsonObjectConverter.convert(courtExtractRequested);
    }

    private static JsonEnvelope getApplicationByIdQuery(final String applicationId, final UUID userId) {
        return envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(APPLICATION_AT_A_GLANCE_QUERY_NAME)
                        .withUserId(userId.toString())
                        .build(),

                createObjectBuilder()
                        .add("applicationId", applicationId)
                        .build());
    }

    private static JsonEnvelope getProsecutionCaseQuery(final String caseId, final UUID userId) {
        return envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource.PROGRESSION_QUERY_PROSECUTION_CASE)
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .add("caseId", caseId)
                        .build()
        );
    }


}
