package uk.gov.moj.cpp.progression.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
/**
 * @deprecated
 *
 */
@SuppressWarnings({"squid:S1133", "squid:S1213"})
@Deprecated
@Entity
@Table(name = "defendant_bail_document",
                uniqueConstraints = @UniqueConstraint(columnNames = "id"))
public class DefendantBailDocument implements Serializable {

    private static final long serialVersionUID = 2094161633016809223L;

    @Column(name = "id", nullable = false, unique = true)
    @Id
    private UUID id;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "active")
    @Convert(converter = BooleanTFConverter.class)
    private Boolean active = Boolean.FALSE;

    @ManyToOne
    @JoinColumn(name="defendant_id")
    private Defendant defendant;

    public DefendantBailDocument() {
        super();
    }


    public UUID getId() {
        return id;
    }


    public void setId(final UUID id) {
        this.id = id;
    }


    public UUID getDocumentId() {
        return documentId;
    }


    public void setDocumentId(final UUID documentId) {
        this.documentId = documentId;
    }


    public Boolean getActive() {
        return active;
    }

    public Defendant getDefendant() {
        return defendant;
    }


    public void setDefendant(final Defendant defendant) {
        this.defendant = defendant;
    }


    public void setActive(final Boolean active) {
        this.active = active;
    }

    public DefendantBailDocument(final DefendantBailDocumentBuilder builder) {
        this.id = builder.id;
        this.documentId = builder.documentId;
        this.active = builder.active;
        this.defendant = builder.defendant;
    }


    public static class DefendantBailDocumentBuilder {

        private UUID id;
        private UUID documentId;
        private Boolean active = Boolean.FALSE;
        private Defendant defendant;

        public DefendantBailDocument build() {
            return new DefendantBailDocument(this);
        }

        public DefendantBailDocumentBuilder setId(final UUID id) {
            this.id = id;
            return this;
        }

        public DefendantBailDocumentBuilder setDocumentId(final UUID documentId) {
            this.documentId = documentId;
            return this;
        }

        public DefendantBailDocumentBuilder setActive(final Boolean active) {
            this.active = active;
            return this;
        }

        public DefendantBailDocumentBuilder setDefendant(final Defendant defendant) {
            this.defendant = defendant;
            return this;
        }
    }

}
