package uk.gov.moj.cpp.progression.domain.event.defendant;


import java.io.Serializable;
import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class BailDocument implements Serializable {

    private static final long serialVersionUID = 1L;
    private final UUID id;
    private final UUID materialId;

    public BailDocument(final UUID id, final UUID materialId) {
        this.id = id;
        this.materialId = materialId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMaterialId() {
        return materialId;
    }
}
