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
		close();
		
		_closed = false;
		_conn = conn;
		
		try {
			_conn.setAutoCommit(false);
		}
		catch(Exception ex) {
			ex.printStackTrace();
			_closed = true;
			_conn.close();
			_conn = null;
			throw ex;
		}
	}

	public void commit() throws SQLException {
		try {
			_conn.commit();
		}
		catch(Exception ex) {
			ex.printStackTrace();
			try { _conn.rollback(); }
			catch(Exception e2) {
				e2.printStackTrace();
			}
			throw ex;
		}
	}

	public void rollback() throws SQLException {
		try {
			if (_conn != null) {
				_conn.rollback();
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	public void exec(String query) throws SQLException {
        try (PreparedStatement pst = _conn.prepareStatement(query)) {
        	pst.executeUpdate();
            pst.close();
        }
	}

	public void close() {
		try {
			if (!_closed) {
				_closed = true;
				_conn.close();
				_conn = null;
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
