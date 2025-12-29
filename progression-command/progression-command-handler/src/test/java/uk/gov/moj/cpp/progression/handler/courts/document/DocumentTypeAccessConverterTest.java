package uk.gov.moj.cpp.progression.handler.courts.document;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.referencedata.json.schemas.CourtDocumentTypeRBAC;
import uk.gov.moj.cpp.referencedata.json.schemas.DeleteUserGroups;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;
import uk.gov.moj.cpp.referencedata.json.schemas.DownloadUserGroups;
import uk.gov.moj.cpp.referencedata.json.schemas.ReadUserGroups;
import uk.gov.moj.cpp.referencedata.json.schemas.UploadUserGroups;

import java.io.InputStream;
import java.util.List;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentTypeAccessConverterTest {

    @Spy
    @SuppressWarnings("unused")
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @InjectMocks
    private DocumentTypeAccessConverter documentTypeAccessConverter;

    @Test
    public void shouldParseJsonObjectToDocumentTypeAccess() throws Exception {

        try (final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("json/referencedata.query.document-type-access.json")) {

            final JsonObject jsonObject = createReader(inputStream).readObject();

            final DocumentTypeAccess documentTypeAccess = documentTypeAccessConverter.toDocumentTypeAccess(jsonObject);

            assertThat(documentTypeAccess.getId(), is(fromString("df2c2845-5db8-4241-bb94-62d821bc02ae")));
            assertThat(documentTypeAccess.getSection(), is("HEARING"));
            assertThat(documentTypeAccess.getDocumentCategory(), is("documentCategory"));
            assertThat(documentTypeAccess.getActionRequired(), is(true));
            assertThat(documentTypeAccess.getSeqNum(), is(10));
            assertThat(documentTypeAccess.getValidFrom(), is("1980-05-13"));
            assertThat(documentTypeAccess.getValidTo(), is("1999-05-13"));
            assertThat(documentTypeAccess.getJurisdiction(), is("CROWN"));
            assertThat(documentTypeAccess.getSentToCps(), is(true));

            final CourtDocumentTypeRBAC courtDocumentTypeRBAC = documentTypeAccess.getCourtDocumentTypeRBAC();

            final List<ReadUserGroups> readUserGroups = courtDocumentTypeRBAC.getReadUserGroups();
            assertThat(readUserGroups.size(), is(1));
            assertThat(readUserGroups.get(0).getValidFrom(), is("1983-05-13"));
            assertThat(readUserGroups.get(0).getValidTo(), is("1983-05-14"));
            assertThat(readUserGroups.get(0).getCppGroup().getId(), is(fromString("ff9fb2c8-2738-4d77-87e5-56b5781b4111")));
            assertThat(readUserGroups.get(0).getCppGroup().getGroupName(), is("Crown Court"));

            final List<UploadUserGroups> uploadUserGroups = courtDocumentTypeRBAC.getUploadUserGroups();
            assertThat(uploadUserGroups.size(), is(1));
            assertThat(uploadUserGroups.get(0).getValidFrom(), is("1983-05-15"));
            assertThat(uploadUserGroups.get(0).getValidTo(), is("1983-05-16"));
            assertThat(uploadUserGroups.get(0).getCppGroup().getId(), is(fromString("ff9fb2c8-2738-4d77-87e5-56b5781b4112")));
            assertThat(uploadUserGroups.get(0).getCppGroup().getGroupName(), is("Aromatherapists"));

            final List<DownloadUserGroups> downloadUserGroups = courtDocumentTypeRBAC.getDownloadUserGroups();
            assertThat(downloadUserGroups.size(), is(1));
            assertThat(downloadUserGroups.get(0).getValidFrom(), is("1983-05-17"));
            assertThat(downloadUserGroups.get(0).getValidTo(), is("1983-05-18"));
            assertThat(downloadUserGroups.get(0).getCppGroup().getId(), is(fromString("ff9fb2c8-2738-4d77-87e5-56b5781b4113")));
            assertThat(downloadUserGroups.get(0).getCppGroup().getGroupName(), is("People called Gerald"));

            final List<DeleteUserGroups> deleteUserGroups = courtDocumentTypeRBAC.getDeleteUserGroups();
            assertThat(deleteUserGroups.size(), is(1));
            assertThat(deleteUserGroups.get(0).getValidFrom(), is("1983-05-19"));
            assertThat(deleteUserGroups.get(0).getValidTo(), is("1983-05-20"));
            assertThat(deleteUserGroups.get(0).getCppGroup().getId(), is(fromString("ff9fb2c8-2738-4d77-87e5-56b5781b4114")));
            assertThat(deleteUserGroups.get(0).getCppGroup().getGroupName(), is("Punk Rockers"));
        }
    }
}
