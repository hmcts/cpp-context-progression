package uk.gov.moj.cpp.progression.handler.courts.document;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentTypeRBACFactoryTest {

    @Mock
    private DocumentUserGroupFinder documentUserGroupFinder;

    @InjectMocks
    private DocumentTypeRBACFactory documentTypeRBACFactory;

    @Test
    public void shouldCreateDocumentTypeRBACWithTheCorrectUserGroups() throws Exception {

        final List<String> uploadUserGroups = singletonList("upload user group");
        final List<String> readUserGroups = singletonList("read user group");
        final List<String> downloadUserGroups = singletonList("download user group");
        final List<String> deleteUserGroups = singletonList("delete user group");

        final DocumentTypeAccess documentTypeAccess = mock(DocumentTypeAccess.class);

        when(documentUserGroupFinder.getUploadUserGroupsFrom(documentTypeAccess)).thenReturn(uploadUserGroups);
        when(documentUserGroupFinder.getReadUserGroupsFrom(documentTypeAccess)).thenReturn(readUserGroups);
        when(documentUserGroupFinder.getDownloadUserGroupsFrom(documentTypeAccess)).thenReturn(downloadUserGroups);
        when(documentUserGroupFinder.getDeleteUserGroupsFrom(documentTypeAccess)).thenReturn(deleteUserGroups);

        final DocumentTypeRBAC documentTypeRBAC = documentTypeRBACFactory.createFromMaterialUserGroups(documentTypeAccess);

        assertThat(documentTypeRBAC.getUploadUserGroups(), is(uploadUserGroups));
        assertThat(documentTypeRBAC.getReadUserGroups(), is(readUserGroups));
        assertThat(documentTypeRBAC.getDownloadUserGroups(), is(downloadUserGroups));
        assertThat(documentTypeRBAC.getDeleteUserGroups(), is(deleteUserGroups));
    }
}
