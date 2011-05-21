package com.floyd.bukkit.petition;

import java.sql.*;
import java.util.Date;

public class DbConnection {
	Long connected = 0L;
	Long leased = 0L;
	Integer count = 0;
	Connection connection = null;
	
	public boolean connect( String url, String login, String password ) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection(url, login, password);
			connection.setAutoCommit(true);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		connected = (new Date()).getTime();
		return true;
	}
	
	public void disconnect() {
		connected = 0L;
		leased = 0L;
		count = 0;
		
	}
	
	public boolean isAvailable() {
		return (leased == 0L);
	}
	
	public boolean isConnected() {
		if (connected == 0L) {
			return false;
		}
		try {
			Boolean valid = connection.isValid(1);
			return valid;
		}
		catch (Exception e){
			return false;
		}
	}
	
	public Connection getConnection() {
		leased = (new Date()).getTime();
		count++;
		return connection;
	}
	
	public boolean releaseConnection(Connection handle) {
		if (connection.equals(handle)) {
			leased = 0L;
			return true;
		} else {
			return false;
		}
	}
	
}
