package uk.gov.moj.cpp.progression.domain.pojo;

import java.io.Serializable;
import java.util.UUID;

@SuppressWarnings({"squid:S2384", "squid:S1213"})
public class ReferenceDataDirectionManagementType implements Serializable {
    private static final long serialVersionUID = -1904682340467152548L;

    private UUID id;
    private String name;
    private Integer sequenceNumber;
    private String formType;
    private String variant;
    private String category;
    private String dataType;

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public String getFormType() {
        return formType;
    }

    public String getVariant() {
        return variant;
    }

    public String getCategory() {
        return category;
    }

    public String getDataType() { return dataType; }

    public ReferenceDataDirectionManagementType(final UUID id, final String name, final Integer sequenceNumber, final String formType, final String variant, final String category,
                                                final String dataType) {
        this.id = id;
        this.name = name;
        this.sequenceNumber = sequenceNumber;
        this.formType = formType;
        this.variant = variant;
        this.category = category;
        this.dataType = dataType;
    }
}
