package com.steamcraftmc.EssentiallyStats.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

import org.bukkit.configuration.InvalidConfigurationException;

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
    private List<StatsTable> tables;

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

    private Connection newConn() throws SQLException {
    	return java.sql.DriverManager.getConnection(
                "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?autoReconnect=true",
                this.username, this.password);
    }
    
    public Connection getConn() {
        if (!isConnected()) {
            try {
                this.conn = newConn();
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

	public MyTransaction beginTransaction() throws SQLException {
		Connection conn = newConn();
		return new MyTransaction(conn);
	}

    private void exec(final String query) throws SQLException {
        try (PreparedStatement pst = getConn().prepareStatement(query)) {
	        pst.executeUpdate();
	        pst.close();
        }
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
	
	private String bungeeServer() throws InvalidConfigurationException {
		return plugin.Config.getBungeeServerName().replaceAll("[^a-zA-Z0-9_]+", "_");
	}

	public List<StatsTable> getTables() {
		return this.tables;
	}

    private Set<String> getFieldNames(String tableName) throws SQLException {
    	HashSet<String> cols = new HashSet<String>(); 
		try (ResultSet rs = this.getResult("SELECT * FROM `" + tableName + "` LIMIT 1;")) {
			if (rs != null) {
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();
				for (int ix = 1; ix <= columnCount; ix++) {
					cols.add(rsmd.getColumnLabel(ix).toLowerCase());
				}
			}
		}
		
		return Collections.unmodifiableSet(cols);
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
				String playerName = t.hasPlayerName() ? "  `player_name` VARCHAR(63) NOT NULL, \n" : "";
				exec("CREATE TABLE IF NOT EXISTS `" + t.TableName + "` ( \n" +
					"  `uuid` VARCHAR(40) NOT NULL, \n" + 
					bungeeKey + 
					"  `created` BIGINT NOT NULL, \n" + 
					"  `updated` BIGINT NOT NULL, \n" + 
					playerName + pk + ");"
				);
			}
		}
		catch(SQLException ex) {
			ex.printStackTrace();
			plugin.log(Level.WARNING, "********* WARNING ********* Unable to create MySql schema");
			return false;
		}

		this.tables = Collections.unmodifiableList(tables);
		return true;
	}

	public void update(MyTransaction trans, StatsTable table, UUID playerUUID, ArrayList<FieldUpdate> updates) throws Exception {
		String preparedSql = prepareUpdate(table, playerUUID, updates);
		//plugin.log(Level.INFO, preparedSql);

		try {
			trans.exec(preparedSql);
		}
		catch(SQLException sqe) {
			if (sqe.getErrorCode() == 1054) {
				trans.commit();
				updateSchema(table, playerUUID, updates);
				
				trans.restart(newConn());
				trans.exec(preparedSql);
				return;
			}

			plugin.log(Level.WARNING, "SQL ERROR #" + sqe.getErrorCode() + ": " + preparedSql + "\n" + sqe.getMessage());
			throw sqe;
		}
	}

	private void updateSchema(StatsTable table, UUID playerUUID, ArrayList<FieldUpdate> updates) throws Exception {
		Set<String> existingCols = getFieldNames(table.TableName);
		for (int ix = 0; ix < updates.size(); ix++) {
			FieldUpdate updt = updates.get(ix);
			if (existingCols.contains(updt.FieldName)) {
				continue;
			}
			
			String alter = String.format(
					"ALTER TABLE `%s` ADD COLUMN %s %s NOT NULL%s;",
					table.TableName, updt.getField(), updt.getFieldType(),
					updt.getFieldType() == "BIGINT" ? " DEFAULT 0" : ""
					);
			try {
				exec(alter);
			}
			catch (SQLException sqe) {
				if (sqe.getErrorCode() != 1060) {
					plugin.log(Level.WARNING, "SQL ERROR #" + sqe.getErrorCode() + ": " + alter + "\n" + sqe.getMessage());
				}
			}
		}
	}

	private String prepareUpdate(StatsTable table, UUID playerUUID, ArrayList<FieldUpdate> updates) throws Exception {
		long now = System.currentTimeMillis() / 1000;
		StringBuilder sb = new StringBuilder();

		// Begin INSERT
		sb.append("INSERT INTO ");
		sb.append(table.TableName);
		sb.append('(');
		sb.append("`uuid`");
		sb.append(",`created`");
		sb.append(",`updated`");
		if (plugin.Config.bungeeSupport()) {
			sb.append(",`server`");
		}
		for (int ix = 0; ix < updates.size(); ix++) {
			sb.append(',');
			sb.append(updates.get(ix).getField());
		}
		sb.append(") \n VALUES('");
		sb.append(playerUUID.toString());
		sb.append('\'');
		sb.append(',');
		sb.append(now);
		sb.append(',');
		sb.append(now);
		if (plugin.Config.bungeeSupport()) {
			sb.append(",'");
			sb.append(bungeeServer());
			sb.append('\'');
		}
		for (int ix = 0; ix < updates.size(); ix++) {
			sb.append(',');
			sb.append(updates.get(ix).getValue());
		}
		sb.append(") \n ON DUPLICATE KEY UPDATE \n");
		
		// Begin UPDATE
		sb.append("`updated` = ");
		sb.append(now);
		for (int ix = 0; ix < updates.size(); ix++) {
			sb.append(',');
			sb.append(updates.get(ix).getAssignment());
		}
		sb.append(';');
		
		return sb.toString();
	}
}
