package triplifyEngine;

import java.io.File;
import java.io.PrintWriter;
import java.sql.SQLException;

import com.hp.hpl.jena.rdf.model.ResourceFactory;

import de.fuberlin.wiwiss.d2rq.map.Database;

/**
 * Represents a database connection, can be used to generate a JDBC URL, 
 * make a new D2RQ Database or generate a D2RQ Mapping entry.
 */
public class Connection {
	public DBsystem system;
	public String host;
	public String database;
	public String password;
	public String username;

	/**
	 * For construction from JSON.
	 */
    public Connection() {}

	/**
	 * Create Connection to a sqlite file, where
	 * database = file name, host = path
	 */
	public Connection(File sqliteFile) {
		system = DBsystem.sqlite;
        try {
		host = sqliteFile.getParent().replace("\\", "/" );
        } catch (NullPointerException e) {
            //host = sqliteFile.toString();
        }
		database = sqliteFile.getName();

	}
	
	/**
	 * Generate a JDBC URL specific to DBsystem.
	 * 
     * @return JDBC URL.
	 */
	String getJdbcUrl() {
		switch(system) {
			case mysql:
				return "jdbc:mysql://" + host + "/" + database;
			case postgresql:
				return "jdbc:postgresql://" + host + "/" + database;
			case oracle:
				return "jdbc:oracle:thin:@" + host + ":" + database;
			case sqlserver:
				return "jdbc:sqlserver://" + host + ";databaseName=" + database;
			case sqlite:
				return "jdbc:sqlite:" + host + "/" + database;
		}
		return null;
	}
	
	/**
	 * Create D2RQ Database from the database connection.
	 * 
     * @return D2RQ Database.
	 */
	public Database getD2RQdatabase() {
		Database database = new Database(ResourceFactory.createResource());
		database.setJDBCDSN(getJdbcUrl());
		database.setUsername(username);
		database.setPassword(password);
		return database;
	}
	
    /**
     * Generate D2RQ Mapping Language representation of this Connection.
     *
     * @param pw PrintWriter used to write output to.
     */
	void printD2RQ(PrintWriter pw) throws SQLException {
		pw.println("map:database a d2rq:Database;");
		pw.println("\td2rq:jdbcDriver \"" + system.driver + "\";");
		pw.println("\td2rq:jdbcDSN \"" + getJdbcUrl() + "\";");
		if (username != null && !username.isEmpty()) 
			pw.println("\td2rq:username \"" + username + "\";");
		if (password != null && !password.isEmpty()) 
			pw.println("\td2rq:password \"" + password + "\";");
		pw.println("\td2rq:fetchSize \"" + (system == DBsystem.mysql ? Integer.MIN_VALUE : 500) + "\";");
		pw.println("\t.");
	}
	
    /**
     * For SQLite DBsystems verify if the file exists 
     * in local filesystem, throw exception if not.
     *
     */
	public void verifyFile() throws Exception {
		if (system.equals(DBsystem.sqlite) && !new File(host + File.separator + database).exists())
			throw new Exception("Data Source file not available.");
	}
	
}
