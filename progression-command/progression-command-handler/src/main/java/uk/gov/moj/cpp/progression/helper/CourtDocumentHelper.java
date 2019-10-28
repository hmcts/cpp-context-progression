package uk.gov.moj.cpp.progression.helper;

import static java.util.Objects.isNull;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Material;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CourtDocumentHelper {

    private CourtDocumentHelper() {
    }

    public static CourtDocument setDefaults(CourtDocument courtDocument) {
        return newBuilderWithDefaults(courtDocument).build();
    }

    private static Builder newBuilderWithDefaults(final CourtDocument copy) {
        Builder builder = new Builder();
        builder.amendmentDate = copy.getAmendmentDate();
        builder.courtDocumentId = copy.getCourtDocumentId();
        builder.documentCategory = copy.getDocumentCategory();
        builder.documentTypeDescription = copy.getDocumentTypeDescription();
        builder.documentTypeId = copy.getDocumentTypeId();
        builder.isRemoved = copy.getIsRemoved();
        builder.materials = copy.getMaterials();
        builder.mimeType = copy.getMimeType();
        builder.name = copy.getName();
        //optional fields with defaults
        builder.containsFinancialMeans = defaultToFalse(copy.getContainsFinancialMeans());
        return builder;
    }

    private static boolean defaultToFalse(final Boolean b) {
        return defaultTo(b, false);
    }

    private static boolean defaultTo(final Boolean b, final boolean defaultValue) {
        if (isNull(b)) {
            return defaultValue;
        } else {
            return b.booleanValue();
        }
    }

    private static final class Builder {
        private LocalDate amendmentDate;
        private Boolean containsFinancialMeans;
        private UUID courtDocumentId;
        private DocumentCategory documentCategory;
        private String documentTypeDescription;
        private UUID documentTypeId;
        private Boolean isRemoved;
        private List<Material> materials;
        private String mimeType;
        private String name;


        public Builder withAmendmentDate(final LocalDate val) {
            amendmentDate = val;
            return this;
        }

        public Builder withContainsFinancialMeans(final Boolean val) {
            containsFinancialMeans = val;
            return this;
        }

        public Builder withCourtDocumentId(final UUID val) {
            courtDocumentId = val;
            return this;
        }

        public Builder withDocumentCategory(final DocumentCategory val) {
            documentCategory = val;
            return this;
        }

        public Builder withDocumentTypeDescription(final String val) {
            documentTypeDescription = val;
            return this;
        }

        public Builder withDocumentTypeId(final UUID val) {
            documentTypeId = val;
            return this;
        }

        public Builder withIsRemoved(final Boolean val) {
            isRemoved = val;
            return this;
        }

        @SuppressWarnings("squid:S2384")
        public Builder withMaterials(final List<Material> val) {
            materials = val;
            return this;
        }

        public Builder withMimeType(final String val) {
            mimeType = val;
            return this;
        }

        public Builder withName(final String val) {
            name = val;
            return this;
        }

        public uk.gov.justice.core.courts.CourtDocument build() {
            return new uk.gov.justice.core.courts.CourtDocument(amendmentDate, containsFinancialMeans, courtDocumentId, documentCategory, documentTypeDescription, documentTypeId, isRemoved, materials, mimeType, name);
        }
    }
}
