package uk.gov.moj.cpp.progression.material;

import static jakarta.ws.rs.client.ClientBuilder.newClient;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.cpp.progression.material.MaterialUrls.BASE_URI;
import static uk.gov.moj.cpp.progression.material.MaterialUrls.COMMAND_BASE_URI;
import static uk.gov.moj.cpp.progression.material.MaterialUrls.MATERIAL_REQUEST_PATH;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Progression's own client for the three material calls it makes, replacing the material-client jar.
 *
 * Only the operations progression uses are here - fetching a material, fetching it as a PDF, and
 * removing one. The jar also offered metadata lookups and bundle creation, which progression never
 * called; carrying those over would be copying code with no consumer.
 *
 * The requests are byte-for-byte the ones material-client made: same URIs, same query parameters,
 * same media types, same CJSCPPUID header. Material's published REST contract is what is being
 * depended on, which is expected between contexts - compiling against its Java classes is not, and
 * that is what forced a release order and blocked the Jakarta upgrade.
 */
@ApplicationScoped
public class MaterialClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialClient.class);

    private static final String MATERIAL_MEDIA_TYPE = "application/vnd.material.query.material+json";
    private static final String REMOVE_MATERIAL_MEDIA_TYPE = "application/vnd.material.command.delete-material+json";
    private static final String REQUEST_PARAM_STREAM = "stream";
    private static final String REQUEST_PARAM_REQUEST_PDF = "requestPdf";

    public static final String REQUEST_PARAM_ADD_INLINE_CONTENT_DISPOSITION_HEADER =
            "addInlineContentDispositionHeader";

    public Response getMaterial(final UUID materialId, final UUID userId) {
        return getMaterial(materialId.toString(), userId.toString(), true, false, false);
    }

    public Response getMaterialAsPdf(final UUID materialId, final UUID userId) {
        return getMaterial(materialId.toString(), userId.toString(), true, true, false);
    }

    /**
     * Not a convenience overload of the UUID form - it asks for different things. The original jar
     * requested this one without streaming and with the inline content-disposition header set, and
     * the query-api resources that serve a document inline rely on that.
     */
    public Response getMaterialAsPdf(final String materialId, final String userId) {
        return getMaterial(materialId, userId, false, true, true);
    }

    public Response removeMaterial(final UUID materialId, final UUID userId, final JsonObject body) {
        final Invocation.Builder builder = getClient()
                .target(COMMAND_BASE_URI)
                .path(MATERIAL_REQUEST_PATH + materialId)
                .request()
                .header(USER_ID, userId.toString())
                .accept(REMOVE_MATERIAL_MEDIA_TYPE);

        LOGGER.info("Invoking call to material context");
        return builder.post(Entity.entity(body, REMOVE_MATERIAL_MEDIA_TYPE));
    }

    private Response getMaterial(final String materialId,
                                 final String userId,
                                 final boolean asStream,
                                 final boolean asPdf,
                                 final boolean addInlineContentDispositionHeader) {
        final Invocation.Builder builder = getClient()
                .target(BASE_URI)
                .path(MATERIAL_REQUEST_PATH + materialId)
                .queryParam(REQUEST_PARAM_STREAM, asStream)
                .queryParam(REQUEST_PARAM_REQUEST_PDF, asPdf)
                .queryParam(REQUEST_PARAM_ADD_INLINE_CONTENT_DISPOSITION_HEADER, addInlineContentDispositionHeader)
                .request()
                .header(USER_ID, userId)
                .accept(MATERIAL_MEDIA_TYPE);

        LOGGER.info("Invoking call to material context");
        return builder.get();
    }

    /**
     * Package-private so a test can substitute the JAX-RS client without a running material service.
     */
    Client getClient() {
        return newClient();
    }
}
