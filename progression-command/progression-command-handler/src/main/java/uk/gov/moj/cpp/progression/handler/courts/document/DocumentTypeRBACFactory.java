package uk.gov.moj.cpp.progression.handler.courts.document;

import static uk.gov.justice.core.courts.DocumentTypeRBAC.documentTypeRBAC;

import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;

import java.util.List;

import javax.inject.Inject;

public class DocumentTypeRBACFactory {

    @Inject
    private DocumentUserGroupFinder documentUserGroupFinder;

    public DocumentTypeRBAC createFromMaterialUserGroups(final DocumentTypeAccess documentTypeAccess) {

        final List<String> uploadUserGroups = documentUserGroupFinder.getUploadUserGroupsFrom(documentTypeAccess);
        final List<String> readUserGroups = documentUserGroupFinder.getReadUserGroupsFrom(documentTypeAccess);
        final List<String> downloadUserGroups = documentUserGroupFinder.getDownloadUserGroupsFrom(documentTypeAccess);
        final List<String> deleteUserGroups = documentUserGroupFinder.getDeleteUserGroupsFrom(documentTypeAccess);

        return documentTypeRBAC()
                .withUploadUserGroups(uploadUserGroups)
                .withReadUserGroups(readUserGroups)
                .withDownloadUserGroups(downloadUserGroups)
                .withDeleteUserGroups(deleteUserGroups).build();
    }
}
