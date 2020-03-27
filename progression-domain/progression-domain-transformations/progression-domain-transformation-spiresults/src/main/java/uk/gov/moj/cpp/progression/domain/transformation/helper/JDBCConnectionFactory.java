package uk.gov.moj.cpp.progression.domain.transformation.helper;

import static java.lang.String.format;
import static java.lang.System.getProperty;

import uk.gov.moj.cpp.progression.domain.transformation.exception.TransformationException;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class JDBCConnectionFactory {

    private static final String JNDI = "java:/app/event-tool/DS.eventstore";
    private Connection connection;

    public Connection getConnection() {
        try {
            if (null == connection || connection.isClosed()) {
                final String jndiCommandLineParameter = getProperty("jndi_name");
                final InitialContext context = new InitialContext();
                final DataSource dataSource = (DataSource) context.lookup(null == jndiCommandLineParameter ? JNDI : jndiCommandLineParameter);
                connection = dataSource.getConnection();
            }
        } catch (NamingException | SQLException e) {
            throw new TransformationException(format("JNDI lookup error: %s", e));
        }
        return connection;
    }
}
