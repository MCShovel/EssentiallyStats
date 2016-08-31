package com.steamcraftmc.EssentiallyStats.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MyTransaction {

	private boolean _closed;
	private Connection _conn;
	public MyTransaction(Connection conn) throws SQLException {
		_closed = true;
		restart(conn);
	}

	public void restart(Connection conn) throws SQLException {
		if (!_closed) {
			try { _conn.close(); } 
			catch (SQLException e) { }
		}
		_closed = false;
		_conn = conn;
		try {
			_conn.setAutoCommit(false);
		}
		catch(Exception ex) {
			_closed = true;
			_conn.close();
			throw ex;
		}
	}

	public void commit() throws SQLException {
		if (_closed) {
			return;
		}
		
		try {
			_conn.commit();
		}
		catch(Exception ex) {
			try { _conn.rollback(); }
			catch(Exception e2) { }
			throw ex;
		}
	}

	public void rollback() throws SQLException {
		if (_closed) {
			return;
		}
		
		try {
			_conn.rollback();
		}
		catch(Exception ex) {
			_closed = true;
			_conn.close();
			throw ex;
		}
	}

	public void exec(String query) throws SQLException {
        try (PreparedStatement pst = _conn.prepareStatement(query)) {
        	pst.executeUpdate();
            pst.close();
        }
	}
}
