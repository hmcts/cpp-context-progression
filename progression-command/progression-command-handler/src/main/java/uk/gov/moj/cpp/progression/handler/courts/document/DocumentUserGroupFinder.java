package uk.gov.moj.cpp.progression.handler.courts.document;

import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.referencedata.json.schemas.DeleteUserGroups;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;
import uk.gov.moj.cpp.referencedata.json.schemas.DownloadUserGroups;
import uk.gov.moj.cpp.referencedata.json.schemas.ReadUserGroups;
import uk.gov.moj.cpp.referencedata.json.schemas.UploadUserGroups;

import java.util.List;

public class DocumentUserGroupFinder {

    public List<String> getReadUserGroupsFrom(final DocumentTypeAccess documentTypeAccess) {

        final List<ReadUserGroups> readUserGroups = documentTypeAccess.getCourtDocumentTypeRBAC().getReadUserGroups();

        return readUserGroups.stream()
                .map(readUserGroup -> readUserGroup.getCppGroup().getGroupName())
                .collect(toList());
    }

    public List<String> getUploadUserGroupsFrom(final DocumentTypeAccess documentTypeAccess) {

        final List<UploadUserGroups> uploadUserGroups = documentTypeAccess.getCourtDocumentTypeRBAC().getUploadUserGroups();

        return uploadUserGroups.stream()
                .map(readUserGroup -> readUserGroup.getCppGroup().getGroupName())
                .collect(toList());
    }

    public List<String> getDownloadUserGroupsFrom(final DocumentTypeAccess documentTypeAccess) {

        final List<DownloadUserGroups> downloadUserGroups = documentTypeAccess.getCourtDocumentTypeRBAC().getDownloadUserGroups();

        return downloadUserGroups.stream()
                .map(readUserGroup -> readUserGroup.getCppGroup().getGroupName())
                .collect(toList());
    }

    public List<String> getDeleteUserGroupsFrom(final DocumentTypeAccess documentTypeAccess) {

        final List<DeleteUserGroups> deleteUserGroups = documentTypeAccess.getCourtDocumentTypeRBAC().getDeleteUserGroups();

        return deleteUserGroups.stream()
                .map(readUserGroup -> readUserGroup.getCppGroup().getGroupName())
                .collect(toList());
    }
}
