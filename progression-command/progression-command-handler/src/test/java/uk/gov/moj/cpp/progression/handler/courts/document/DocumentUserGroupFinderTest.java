package uk.gov.moj.cpp.progression.handler.courts.document;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.referencedata.json.schemas.CourtDocumentTypeRBAC;
import uk.gov.moj.cpp.referencedata.json.schemas.CppGroup;
import uk.gov.moj.cpp.referencedata.json.schemas.DeleteUserGroups;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;
import uk.gov.moj.cpp.referencedata.json.schemas.DownloadUserGroups;
import uk.gov.moj.cpp.referencedata.json.schemas.ReadUserGroups;
import uk.gov.moj.cpp.referencedata.json.schemas.UploadUserGroups;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentUserGroupFinderTest {

    @InjectMocks
    private DocumentUserGroupFinder documentUserGroupFinder;

    @Test
    public void shouldGetTheReadUserGroupNames() throws Exception {
        final DocumentTypeAccess documentTypeAccess = mock(DocumentTypeAccess.class);
        final CourtDocumentTypeRBAC courtDocumentTypeRBAC = mock(CourtDocumentTypeRBAC.class);
        final ReadUserGroups readUserGroups_1 = mock(ReadUserGroups.class);
        final ReadUserGroups readUserGroups_2 = mock(ReadUserGroups.class);
        final CppGroup cppGroup_1 = mock(CppGroup.class);
        final CppGroup cppGroup_2 = mock(CppGroup.class);

        when(documentTypeAccess.getCourtDocumentTypeRBAC()).thenReturn(courtDocumentTypeRBAC);
        when(courtDocumentTypeRBAC.getReadUserGroups()).thenReturn(asList(readUserGroups_1, readUserGroups_2));
        when(readUserGroups_1.getCppGroup()).thenReturn(cppGroup_1);
        when(cppGroup_1.getGroupName()).thenReturn("readGroup_1");
        when(readUserGroups_2.getCppGroup()).thenReturn(cppGroup_2);
        when(cppGroup_2.getGroupName()).thenReturn("readGroup_2");

        final List<String> readUserGroups = documentUserGroupFinder.getReadUserGroupsFrom(documentTypeAccess);

        assertThat(readUserGroups.size(), is(2));
        assertThat(readUserGroups.get(0), is("readGroup_1"));
        assertThat(readUserGroups.get(1), is("readGroup_2"));
    }

    @Test
    public void shouldGetTheUploadUserGroupNames() throws Exception {
        final DocumentTypeAccess documentTypeAccess = mock(DocumentTypeAccess.class);
        final CourtDocumentTypeRBAC courtDocumentTypeRBAC = mock(CourtDocumentTypeRBAC.class);
        final UploadUserGroups uploadUserGroups_1 = mock(UploadUserGroups.class);
        final UploadUserGroups uploadUserGroups_2 = mock(UploadUserGroups.class);
        final CppGroup cppGroup_1 = mock(CppGroup.class);
        final CppGroup cppGroup_2 = mock(CppGroup.class);

        when(documentTypeAccess.getCourtDocumentTypeRBAC()).thenReturn(courtDocumentTypeRBAC);
        when(courtDocumentTypeRBAC.getUploadUserGroups()).thenReturn(asList(uploadUserGroups_1, uploadUserGroups_2));
        when(uploadUserGroups_1.getCppGroup()).thenReturn(cppGroup_1);
        when(cppGroup_1.getGroupName()).thenReturn("uploadGroup_1");
        when(uploadUserGroups_2.getCppGroup()).thenReturn(cppGroup_2);
        when(cppGroup_2.getGroupName()).thenReturn("uploadGroup_2");

        final List<String> readUserGroups = documentUserGroupFinder.getUploadUserGroupsFrom(documentTypeAccess);

        assertThat(readUserGroups.size(), is(2));
        assertThat(readUserGroups.get(0), is("uploadGroup_1"));
        assertThat(readUserGroups.get(1), is("uploadGroup_2"));
    }

    @Test
    public void shouldGetTheDownloadUserGroupNames() throws Exception {
        final DocumentTypeAccess documentTypeAccess = mock(DocumentTypeAccess.class);
        final CourtDocumentTypeRBAC courtDocumentTypeRBAC = mock(CourtDocumentTypeRBAC.class);
        final DownloadUserGroups downloadUserGroups_1 = mock(DownloadUserGroups.class);
        final DownloadUserGroups downloadUserGroups_2 = mock(DownloadUserGroups.class);
        final CppGroup cppGroup_1 = mock(CppGroup.class);
        final CppGroup cppGroup_2 = mock(CppGroup.class);

        when(documentTypeAccess.getCourtDocumentTypeRBAC()).thenReturn(courtDocumentTypeRBAC);
        when(courtDocumentTypeRBAC.getDownloadUserGroups()).thenReturn(asList(downloadUserGroups_1, downloadUserGroups_2));
        when(downloadUserGroups_1.getCppGroup()).thenReturn(cppGroup_1);
        when(cppGroup_1.getGroupName()).thenReturn("downloadGroup_1");
        when(downloadUserGroups_2.getCppGroup()).thenReturn(cppGroup_2);
        when(cppGroup_2.getGroupName()).thenReturn("downloadGroup_2");

        final List<String> readUserGroups = documentUserGroupFinder.getDownloadUserGroupsFrom(documentTypeAccess);

        assertThat(readUserGroups.size(), is(2));
        assertThat(readUserGroups.get(0), is("downloadGroup_1"));
        assertThat(readUserGroups.get(1), is("downloadGroup_2"));
    }

    @Test
    public void shouldGetTheDeleteUserGroupNames() throws Exception {
        final DocumentTypeAccess documentTypeAccess = mock(DocumentTypeAccess.class);
        final CourtDocumentTypeRBAC courtDocumentTypeRBAC = mock(CourtDocumentTypeRBAC.class);
        final DeleteUserGroups deleteUserGroups_1 = mock(DeleteUserGroups.class);
        final DeleteUserGroups deleteUserGroups_2 = mock(DeleteUserGroups.class);
        final CppGroup cppGroup_1 = mock(CppGroup.class);
        final CppGroup cppGroup_2 = mock(CppGroup.class);

        when(documentTypeAccess.getCourtDocumentTypeRBAC()).thenReturn(courtDocumentTypeRBAC);
        when(courtDocumentTypeRBAC.getDeleteUserGroups()).thenReturn(asList(deleteUserGroups_1, deleteUserGroups_2));
        when(deleteUserGroups_1.getCppGroup()).thenReturn(cppGroup_1);
        when(cppGroup_1.getGroupName()).thenReturn("deleteGroup_1");
        when(deleteUserGroups_2.getCppGroup()).thenReturn(cppGroup_2);
        when(cppGroup_2.getGroupName()).thenReturn("deleteGroup_2");

        final List<String> readUserGroups = documentUserGroupFinder.getDeleteUserGroupsFrom(documentTypeAccess);

        assertThat(readUserGroups.size(), is(2));
        assertThat(readUserGroups.get(0), is("deleteGroup_1"));
        assertThat(readUserGroups.get(1), is("deleteGroup_2"));
    }
}
