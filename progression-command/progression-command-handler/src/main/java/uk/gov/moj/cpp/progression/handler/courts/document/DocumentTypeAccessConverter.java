package uk.gov.moj.cpp.progression.handler.courts.document;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;

import jakarta.inject.Inject;
import jakarta.json.JsonObject;

public class DocumentTypeAccessConverter {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    public DocumentTypeAccess toDocumentTypeAccess(final JsonObject jsonObject) {
        return jsonObjectToObjectConverter.convert(jsonObject, DocumentTypeAccess.class);
    }
}
