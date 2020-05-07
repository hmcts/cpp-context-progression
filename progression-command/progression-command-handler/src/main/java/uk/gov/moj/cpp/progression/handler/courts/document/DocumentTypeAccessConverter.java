package uk.gov.moj.cpp.progression.handler.courts.document;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;

import javax.inject.Inject;
import javax.json.JsonObject;

public class DocumentTypeAccessConverter {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    public DocumentTypeAccess toDocumentTypeAccess(final JsonObject jsonObject) {
        return jsonObjectToObjectConverter.convert(jsonObject, DocumentTypeAccess.class);
    }
}
