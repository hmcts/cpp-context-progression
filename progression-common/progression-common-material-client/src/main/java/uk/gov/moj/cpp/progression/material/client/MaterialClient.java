package uk.gov.moj.cpp.progression.material.client;

import static javax.ws.rs.client.ClientBuilder.newClient;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

/**
 * Progression-owned copy of material's HTTP adapter (decouples progression from the material-client
 * JAR - PEG-3444). Trimmed to the methods progression uses; runtime behaviour is unchanged -
 * progression still calls material over HTTP exactly as before.
 */
@ApplicationScoped
public class MaterialClient {

    public static final String REQUEST_PARAM_ADD_INLINE_CONTENT_DISPOSITION_HEADER = "addInlineContentDispositionHeader";

    @Inject
    Logger logger;

    private static final String BASE_URI = "http://localhost:8080/material-query-api/query/api/rest/material";
    private static final String COMMAND_BASE_URI = "http://localhost:8080/material-command-api/command/api/rest/material";
    private static final String MATERIAL_REQUEST_PATH = "/material/";
    private static final String GET_MATERIAL_AS_PDF = "application/vnd.material.query.material+json";
    private static final String REMOVE_MATERIAL = "application/vnd.material.command.delete-material+json";
    private static final String REQUEST_PARAM_STREAM = "stream";
    private static final String REQUEST_PARAM_REQUEST_PDF = "requestPdf";

    public Response getMaterialAsPdf(final String materialId, final String userId) {
        return getMaterial(materialId, userId, false, true, true);
    }

    public Response getMaterial(final UUID materialId, final UUID userId) {
        return getMaterial(materialId.toString(), userId.toString(), true, false, false);
    }

    public Response removeMaterial(final UUID materialId, final UUID userId, final JsonObject body) {
        return removeMaterial(materialId.toString(), userId.toString(), body);
    }

    private Response getMaterial(final String materialId, final String userId, final boolean asStream, final boolean asPdf, final boolean addInlineContentDispositionHeader) {

        final Invocation.Builder builder = getClient()
                .target(BASE_URI)
                .path(MATERIAL_REQUEST_PATH + materialId)
                .queryParam(REQUEST_PARAM_STREAM, asStream)
                .queryParam(REQUEST_PARAM_REQUEST_PDF, asPdf)
                .queryParam(REQUEST_PARAM_ADD_INLINE_CONTENT_DISPOSITION_HEADER, addInlineContentDispositionHeader)
                .request()
                .header(USER_ID, userId)
                .accept(GET_MATERIAL_AS_PDF);

        logger.info("Invoking call to material context");
        return builder.get();
    }

    private Response removeMaterial(final String materialId, final String userId, final JsonObject body) {

        final Invocation.Builder builder = getClient()
                .target(COMMAND_BASE_URI)
                .path(MATERIAL_REQUEST_PATH + materialId)
                .request()
                .header(USER_ID, userId)
                .accept(REMOVE_MATERIAL);

        logger.info("Invoking call to material context");
        return builder.post(Entity.entity(body, REMOVE_MATERIAL));
    }

    /**
     * Returns the JAX-RS client. Extracted so the client can be mocked in unit tests, since the
     * static {@code newClient()} call is otherwise hard to stub.
     */
    Client getClient() {
        return newClient();
    }
}
