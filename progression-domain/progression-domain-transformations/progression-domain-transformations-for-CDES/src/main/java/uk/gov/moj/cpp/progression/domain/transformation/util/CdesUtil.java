package uk.gov.moj.cpp.progression.domain.transformation.util;

import static java.nio.charset.Charset.defaultCharset;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.CHARGES;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.COURT_FINAL_ORDERS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.DOCUMENTS_TYPE_ACCESS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.DOCUMENT_TYPE_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.POSTAL_NOTIFICATION_DOCUMENT_TYPE_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.REF_DATA_DOCUMENT_TYPE_FILE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CdesTransformationConstants.SECTION;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import java.io.IOException;
import java.util.function.Predicate;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdesUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdesUtil.class);

    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private static final JsonObject typeRefDataObject = getJsonObjectFromFile(REF_DATA_DOCUMENT_TYPE_FILE);
    private static final JsonObject documentTypeRefDataForNows = (JsonObject)typeRefDataObject.getJsonArray(DOCUMENTS_TYPE_ACCESS).stream()
            .filter(getDocumentTypeNOWSPredicate())
            .findFirst().orElse(null);
    private static final JsonObject documentTypeRefDataForPostalNotification = (JsonObject)typeRefDataObject.getJsonArray(DOCUMENTS_TYPE_ACCESS).stream()
            .filter(getDocumentTypePostalOrdersSPredicate())
            .findFirst().orElse(null);
    private static final JsonObject documentTypeRefDataForCharges =(JsonObject)typeRefDataObject.getJsonArray(DOCUMENTS_TYPE_ACCESS).stream()
            .filter(getDocumentTypeChargesPredicate())
            .findFirst().orElse(null);

    private CdesUtil() {
    }

    public static String getPayload(final String path) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(path), defaultCharset());
        } catch (final IOException e) {
            LOGGER.error("Error consuming file from location {}", path, e);
        }
        return request;
    }


    public static JsonObject getJsonObjectFromFile(final String path) {
        return stringToJsonObjectConverter.convert(getPayload(path));
    }

    public static Predicate<JsonValue> getDocumentTypeChargesPredicate() {
        return s-> ((JsonObject)s).getString(SECTION).trim().equalsIgnoreCase(CHARGES);
    }

    private static Predicate<JsonValue> getDocumentTypeNOWSPredicate() {
        return s-> ((JsonObject)s).getString(SECTION).trim().equalsIgnoreCase(COURT_FINAL_ORDERS);
    }

    private static Predicate<JsonValue> getDocumentTypePostalOrdersSPredicate() {
        return def1 -> ((JsonObject) def1).getString(ID).trim().equalsIgnoreCase(POSTAL_NOTIFICATION_DOCUMENT_TYPE_ID);
    }

    public static Predicate<JsonValue> getDocumentTypeIdPredicate(final JsonObject courtDocument) {
        return def1 -> ((JsonObject) def1).getString(ID).trim().equalsIgnoreCase(courtDocument.getString(DOCUMENT_TYPE_ID).trim());
    }

    public static JsonObject getTypeRefDataObject() {
        return typeRefDataObject;
    }

    public static JsonObject getDocumentTypeRefDataForNows() {
        return documentTypeRefDataForNows;
    }

    public static JsonObject getDocumentTypeRefDataForPostalNotification() {
        return documentTypeRefDataForPostalNotification;
    }

    public static JsonObject getDocumentTypeRefDataForCharges() {
        return documentTypeRefDataForCharges;
    }
}