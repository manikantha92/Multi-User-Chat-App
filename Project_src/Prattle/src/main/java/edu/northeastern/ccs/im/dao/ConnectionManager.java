package edu.northeastern.ccs.im.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import edu.northeastern.ccs.im.MyLogger;


/**
 * Use ConnectionManager to connect to your database instance.
 * 
 * ConnectionManager uses the MySQL Connector/J driver to connect to your local MySQL instance.
 * 
 * In our example, we will create a DAO (data access object) java class to interact with
 * each MySQL table. The DAO java classes will use ConnectionManager to open and close
 * connections.
 * 
 * Instructions:
 * 1. Install MySQL Community Server. During installation, you will need to set up a user,
 * password, and port. Keep track of these values.
 * 2. Download and install Connector/J: http://dev.mysql.com/downloads/connector/j/
 * 3. Add the Connector/J JAR to your buildpath. This allows your application to use the
 * Connector/J library. You can add the JAR using either of the following methods:
 *   A. When creating a new Java project, on the "Java Settings" page, go to the 
 *   "Libraries" tab.
 *   Click on the "Add External JARs" button.
 *   Navigate to the Connector/J JAR. On Windows, this looks something like:
 *   C:\Program Files (x86)\MySQL\Connector.J 5.1\mysql-connector-java-5.1.34-bin.jar
 *   B. If you already have a Java project created, then go to your project properties.
 *   Click on the "Java Build Path" option.
 *   Click on the "Libraries" tab, click on the "Add External Jars" button, and
 *   navigate to the Connector/J JAR.
 * 4. Update the "private final" variables below.
 */
public class ConnectionManager {

	// User to connect to your database instance. By default, this is "root".
	private static final String USER = "bad4974fff960f";
	// Password for the user.
	private static final String PW = "67f446c4";
	// URI to your database server. If running on the same machine, then this is "localhost".
	private static final String HOSTNAME = "us-cdbr-iron-east-01.cleardb.net";
	// Port to your database server. By default, this is 3307.
	private static final int PORT = 3306;
	// Name of the MySQL schema that contains your tables.
	private static final String SCHEMA = "heroku_da243c4acb04ba0";
	/** Get the connection to the database instance. */
	public Connection getConnection() {
		Connection connection = null;
		try {
			Properties connectionProperties = new Properties();
			connectionProperties.put("user", USER);
			connectionProperties.put("password", PW);
			Class.forName("com.mysql.jdbc.Driver");

			connection = DriverManager.getConnection(
			    "jdbc:mysql://" + HOSTNAME + ":" + PORT + "/" + SCHEMA,
			    connectionProperties);
		} catch (SQLException | ClassNotFoundException e) {
      MyLogger.log(Level.SEVERE, e.getMessage());
		}
		return connection;
	}
}
