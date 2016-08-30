package com.steamcraftmc.EssentiallyStats.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.Controllers.StatsTable;

public class MySql {

	private final MainPlugin plugin;

    private String host;
    private int port;
    private String username;
    private String password;
    private String database;
    
    private Connection conn;

    public MySql(MainPlugin plugin) {
    	
    	this.plugin = plugin;
    	loadConfig();
    }

    private void loadConfig() {
        this.host = plugin.Config.get("mysql.host", "localhost");
        this.port = plugin.Config.getInt("mysql.port", 3306);
        this.username = plugin.Config.get("mysql.username", "root");
        this.password = plugin.Config.get("mysql.password", "password");
        this.database = plugin.Config.get("mysql.database", "estats");
        closeConnection();
	}

    public Connection getConn() {
        if (!isConnected()) {
            try {
                this.conn = java.sql.DriverManager.getConnection(
                        "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?autoReconnect=true",
                        this.username, this.password);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this.conn;
    }

	public boolean isConnected() {
        try { 
        	return this.conn != null && this.conn.isClosed() == false; 
    	}
        catch(SQLException e) { 
        	this.conn = null; 
        	return false; 
    	}
    }

    public void closeConnection() {
        if (isConnected()) {
            try {
                this.conn.close();
                this.conn = null;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void exec(final String query) throws SQLException {
        PreparedStatement pst = getConn().prepareStatement(query);
        pst.executeUpdate();
        pst.close();
    }

    private ResultSet getResult(String query) {
        try {
            PreparedStatement pst = getConn().prepareStatement(query);
            return pst.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ArrayList<String> getFieldNames(String tableName) throws SQLException {
    	ArrayList<String> cols = new ArrayList<String>(); 
		try (ResultSet rs = this.getResult("SELECT * FROM `" + tableName + "` LIMIT 1;")) {
			if (rs != null) {
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();
				for (int ix = 1; ix <= columnCount; ix++) {
					cols.add(rsmd.getColumnLabel(ix));
				}
			}
		}
		return cols;
    }
    
	public boolean initSchema() {
		try {
			getResult("SELECT 1;").close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			plugin.log(Level.WARNING, "********* WARNING ********* Missing or incorrect MySql configuration");
			return false;
		}
		
		ArrayList<StatsTable> tables = new ArrayList<StatsTable>();
		List<String> tableNames = plugin.Config.getSections("tables");
		for (String name : tableNames) {
			tables.add(new StatsTable(plugin, name));
		}

		String bungeeKey = "";
		String pk = "PRIMARY KEY (`uuid`)";
		if (plugin.Config.bungeeSupport()) {
			bungeeKey = "  `server` VARCHAR(40) NOT NULL, \n";
			pk = "PRIMARY KEY (`uuid`, `server`)";
		}
		
		try {
			for (StatsTable t : tables) {
				String playerName = t.hasPlayerName() ? "  `playerName` VARCHAR(63) NOT NULL, \n" : "";
				exec("CREATE TABLE IF NOT EXISTS `" + t.TableName + "` ( \n" +
					"  `uuid` VARCHAR(40) NOT NULL, \n" + 
					bungeeKey + playerName + pk + ");"
				);
				t.setFieldNames(getFieldNames(t.TableName));
			}
		}
		catch(SQLException ex) {
			ex.printStackTrace();
			plugin.log(Level.WARNING, "********* WARNING ********* Unable to create MySql schema");
			return false;
		}

		return true;
	}
}
