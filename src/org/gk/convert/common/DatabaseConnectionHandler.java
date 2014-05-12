/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.convert.common;

import java.sql.SQLException;

import org.gk.persistence.MySQLAdaptor;

/**
 * A bunch of methods that bundles together useful database connectivity parameters
 * for Reactome databases.
 * 
 * This is basically a throwaway utility class.
 * 
 * @author David Croft
 *
 */
public class DatabaseConnectionHandler {
	private String dbName = "";
	private String hostname = "";
	private String username = "";
	private String port = "";
	private String password = "";
	private MySQLAdaptor databaseAdaptor = null;

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setDatabaseAdaptor(MySQLAdaptor dbAdaptor) {
		this.databaseAdaptor = dbAdaptor;
	}

	public MySQLAdaptor getDatabaseAdaptor() {
		if (!getDbName().isEmpty() || !getHostname().isEmpty() || !getUsername().isEmpty() || !getPort().isEmpty() || !getPassword().isEmpty())
			setDatabaseAdaptor(getDatabaseAdaptor(getDbName(), getHostname(), getUsername(), getPort(), getPassword()));
		
		return databaseAdaptor;
	}
	
	/**
	 * Build a Reactome database adaptor (DBA) using the supplied database parameters.
	 * 
	 * @param dbName
	 * @param hostname
	 * @param username
	 * @param port
	 * @param password
	 * @return
	 */
	public static MySQLAdaptor getDatabaseAdaptor(String dbName, String hostname, String username , String port, String password) {
		MySQLAdaptor dba = null;

		try {
			if (port == null || port.equals(""))
				dba = new MySQLAdaptor(hostname, dbName, username, password);
			else
				dba = new MySQLAdaptor(hostname, dbName, username, password, Integer.parseInt(port));
		} catch (NumberFormatException e) {
			System.err.println("DatabaseConnectionHandler.getDatabaseAdaptor: WARNING - port number is strange: " + port);
			System.err.println("DatabaseConnectionHandler.getDatabaseAdaptor: hostname=" + hostname + ", dbName=" + dbName + ", username" + username + ", password=" + password + ", port=" + port);
			e.printStackTrace();
		} catch (SQLException e) {
			System.err.println("DatabaseConnectionHandler.getDatabaseAdaptor: WARNING - something went wrong with mysql");
			System.err.println("DatabaseConnectionHandler.getDatabaseAdaptor: hostname=" + hostname + ", dbName=" + dbName + ", username" + username + ", password=" + password + ", port=" + port);
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("DatabaseConnectionHandler.getDatabaseAdaptor: WARNING - something went wrong");
			System.err.println("DatabaseConnectionHandler.getDatabaseAdaptor: hostname=" + hostname + ", dbName=" + dbName + ", username" + username + ", password=" + password + ", port=" + port);
			e.printStackTrace();
		}

		return dba;
	}
	
	public boolean parseDatabaseArgument(String[] args, int index) {
		boolean isKnownArgument = true;
		String arg1;
		String arg2;
		arg1 = args[index];
		if (index + 1 >= args.length) {
			System.err.println("DatabaseConnectionHandler.parseDatabaseArgument: WARNING - missing argument after " + arg1);
			return false;
		}
		arg2 = args[index + 1];
		if (arg1.equals("-host"))
			setHostname(arg2);
		else if (arg1.equals("-db"))
			setDbName(arg2);
		else if (arg1.equals("-user"))
			setUsername(arg2);
		else if (arg1.equals("-port"))
			setPort(arg2);
		else if (arg1.equals("-pass"))
			setPassword(arg2);
		else
			isKnownArgument = false;

		return isKnownArgument;
	}
}
