package uk.gov.moj.cpp.prosecutioncase.persistence.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;

@Embeddable
public class CourtDocumentTypeRBAC implements Serializable {

    private static final long serialVersionUID = 1L;

    @ElementCollection
    @CollectionTable(
            name = "document_rbac_read_usergroup",
            joinColumns = @JoinColumn(name = "court_document_id")
    )
    @Column(name = "read_usergroup", nullable = true)
    private List<String> readUserGroups = new ArrayList<>();


    @ElementCollection
    @CollectionTable(
            name = "document_rbac_create_usergroup",
            joinColumns = @JoinColumn(name = "court_document_id")
    )
    @Column(name = "create_usergroup", nullable = true)
    private List<String> createUserGroups = new ArrayList<>();


    @ElementCollection
    @CollectionTable(
            name = "document_rbac_download_usergroup",
            joinColumns = @JoinColumn(name = "court_document_id")
    )
    @Column(name = "download_usergroup", nullable = true)
    private List<String> downloadUserGroups = new ArrayList<>();


    public List<String> getReadUserGroups() {
        return readUserGroups.stream().collect(Collectors.toList());
    }

    public void setReadUserGroups(final List<String> readUserGroups) {
        if (readUserGroups != null && !readUserGroups.isEmpty()) {
            this.readUserGroups = readUserGroups.stream().collect(Collectors.toList());
        }
    }

    public List<String> getCreateUserGroups() {
        return createUserGroups.stream().collect(Collectors.toList());
    }

    public void setCreateUserGroups(final List<String> createUserGroups) {
        if (createUserGroups != null && !createUserGroups.isEmpty()) {
            this.createUserGroups = createUserGroups.stream().collect(Collectors.toList());
        }
    }

    public List<String> getDownloadUserGroups() {
        return downloadUserGroups.stream().collect(Collectors.toList());
    }

    public void setDownloadUserGroups(final List<String> downloadUserGroups) {
        if (downloadUserGroups != null && !downloadUserGroups.isEmpty()) {
            this.downloadUserGroups = downloadUserGroups.stream().collect(Collectors.toList());
        }
    }
}
