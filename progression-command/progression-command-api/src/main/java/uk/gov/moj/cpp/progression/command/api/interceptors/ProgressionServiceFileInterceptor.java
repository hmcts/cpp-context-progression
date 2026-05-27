package uk.gov.moj.cpp.progression.command.api.interceptors;

import static com.azure.core.util.BinaryData.fromStream;
import static com.azure.core.util.Context.NONE;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.adapter.rest.multipart.FileInputDetails;
import uk.gov.justice.services.core.interceptor.Interceptor;
import uk.gov.justice.services.core.interceptor.InterceptorChain;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobParallelUploadOptions;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObjectBuilder;

public class ProgressionServiceFileInterceptor implements Interceptor {

    private static final Duration TRANSFER_TIMEOUT = Duration.ofSeconds(300);

    @Inject
    private BlobContainerClient blobContainerClient;

    @Override
    @SuppressWarnings("unchecked")
    public InterceptorContext process(final InterceptorContext interceptorContext, final InterceptorChain interceptorChain) {
        final Optional<Object> inputParameter = interceptorContext.getInputParameter(FileInputDetails.FILE_INPUT_DETAILS_LIST);
        if (inputParameter.isPresent()) {
            final List<FileInputDetails> fileInputDetailsList = (List<FileInputDetails>) inputParameter.get();
            final Map<String, UUID> results = storeFiles(fileInputDetailsList);
            final JsonEnvelope modifiedEnvelope = addResultsToEnvelope(interceptorContext.inputEnvelope(), results);
            return interceptorChain.processNext(interceptorContext.copyWithInput(modifiedEnvelope));
        }
        return interceptorChain.processNext(interceptorContext);
    }

    private Map<String, UUID> storeFiles(final List<FileInputDetails> fileInputDetailsList) {
        final Map<String, UUID> results = new HashMap<>();
        for (final FileInputDetails fileDetails : fileInputDetailsList) {
            try (final InputStream inputStream = fileDetails.getInputStream()) {
                final UUID fileId = UUID.randomUUID();
                blobContainerClient.getBlobClient(fileId.toString())
                        .uploadWithResponse(
                                new BlobParallelUploadOptions(fromStream(inputStream))
                                        .setMetadata(Map.of("fileName", fileDetails.getFileName().strip())),
                                TRANSFER_TIMEOUT, NONE);
                results.put(fileDetails.getFieldName(), fileId);
            } catch (final IOException e) {
                throw new RuntimeException("Failed to store uploaded file: " + fileDetails.getFileName(), e);
            }
        }
        return results;
    }

    private JsonEnvelope addResultsToEnvelope(final JsonEnvelope envelope, final Map<String, UUID> results) {
        final JsonObjectBuilder payloadBuilder = createObjectBuilder(envelope.payloadAsJsonObject());
        results.forEach((fieldName, fileId) -> payloadBuilder.add(fieldName, fileId.toString()));
        return envelopeFrom(metadataFrom(envelope.metadata()), payloadBuilder.build());
    }
}
