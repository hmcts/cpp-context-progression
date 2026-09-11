package uk.gov.moj.cpp.progression.helper;

import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.StorageException;
import uk.gov.justice.services.jdbc.persistence.DataAccessException;
import uk.gov.justice.services.messaging.JsonObjects;

import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;

public class FileDatastoreHelper {

    private static final String SELECT_CONTENT_SQL = "SELECT content FROM content WHERE file_id = ?";

    public static JsonObject retrieveFileContentByFileId(final UUID fileId) throws FileServiceException {
        try {
            JsonObject fileContent;
            try (final Connection connection = getFileServiceConnection("fileservice");
                    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_CONTENT_SQL)) {
                preparedStatement.setObject(1, fileId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new FileServiceException("No record found");
                    }
                    InputStream contentStream = resultSet.getBinaryStream(1);
                    try (JsonReader reader = JsonObjects.createReader(contentStream)) {
                        fileContent =  reader.readObject();
                    }
                }
            }
            return fileContent;
        } catch (SQLException e) {
            throw new StorageException(String.format("Failed to read content of file with file id %s", fileId), e);
        }
    }
    private static Connection getFileServiceConnection(final String contextName) {
        final String host = getHost();
        final String url = "jdbc:postgresql://" + host + "/fileservice";
        try {
            return DriverManager.getConnection(url, contextName, contextName);
        } catch (SQLException sqlException) {
            throw new DataAccessException("Failed to get JDBC connection to fileservice context", sqlException);
        }
    }
}