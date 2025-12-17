package uk.gov.moj.cpp.progression.query.view.response;



import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class DefendantBailDocumentView {

    private final UUID id;
    private final UUID documentId;
    private final Boolean active;

    public DefendantBailDocumentView(final UUID id, final UUID documentId, final Boolean active) {
        super();
        this.id = id;
        this.documentId = documentId;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public Boolean getActive() {
        return active;
    }



}
