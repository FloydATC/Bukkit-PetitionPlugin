package com.floyd.bukkit.petition;

import java.sql.*;

public class DbPool {
	String db_url = "";
	String db_login = "";
	String db_password = "";
	Integer db_min = 2;
	Integer db_max = 10;
	DbConnection[] connPool = null;
	Integer connected = 0;
	
	public DbPool( String url, String login, String password, Integer min, Integer max ) {
		System.out.println("[Pe]DbPool initializing "+url+" as "+login+" (Size "+min+"-"+max+")");
		
		// Check parameters
		if (db_url == null || db_url.matches("^jdbc\\:")) {
			throw new RuntimeException("Invalid JDBC URL");
		}
		if (db_min < 1) {
			throw new RuntimeException("Minimum pool size must be 1 or greater");
		}
		if (db_max < db_min) {
			throw new RuntimeException("Maximum pool size must equal to or greater than the minimum");
		}
				
		// Assign parameters
		db_url = url;
		db_login = login;
		db_password = password;
		db_min = min;
		db_max = max;
		
		// Initialize connection pool
		connPool = new DbConnection[db_max];
		for (Integer i=0; i<db_max; i++) {
			connPool[i] = new DbConnection();
		}
		for (Integer i=0; i<db_min; i++) {
			if (connPool[i].connect(db_url, db_login, db_password)) {
				connected++;
			} else {
				throw new RuntimeException("Database connection failed");
			}
		}
	}
	
	public synchronized Connection getConnection() {
		for (DbConnection conn : connPool) {
			if (conn.isAvailable()) {
				if (conn.isConnected() == false) {
					if (conn.connect(db_url, db_login, db_password)) {
						connected++;
					} else {
						throw new RuntimeException("Database connection failed");
					}
				}
				if (conn.isConnected()) {
					return conn.getConnection();
				}
			}
		}
		throw new RuntimeException("Database connection pool exhausted");
	}
	
	public synchronized void releaseConnection(Connection handle) {
		for (DbConnection conn : connPool) {
			if (conn.isAvailable() == false && conn.releaseConnection(handle)) {
				return;
			}
		}
		throw new RuntimeException("Database connection handle is unknown or already released");
	}
	
}
