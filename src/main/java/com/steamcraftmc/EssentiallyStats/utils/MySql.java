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
import com.steamcraftmc.EssentiallyStats.Controllers.PlayerStatsInfo;
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

    public void loadConfig() {
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

	private String bungeeServer() throws InvalidConfigurationException {
		return plugin.Config.getBungeeServerName().replaceAll("[^a-zA-Z0-9_]+", "_");
	}

	public List<StatsTable> getTables() {
		return this.tables;
	}
	
	public Set<String> getFields(StatsTable tbl) {
		try {
			return this.getFieldNames(tbl.TableName);
		} catch (SQLException e) {
			e.printStackTrace();
			return new HashSet<String>();
		}
	}

    private Set<String> getFieldNames(String tableName) throws SQLException {
    	HashSet<String> cols = new HashSet<String>(); 
        PreparedStatement pst = getConn().prepareStatement("SELECT * FROM `" + tableName + "` LIMIT 1;");
		try (ResultSet rs = pst.executeQuery()) {
			if (rs != null) {
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();
				for (int ix = 1; ix <= columnCount; ix++) {
					cols.add(rsmd.getColumnLabel(ix).toLowerCase());
				}
				rs.close();
			}
			pst.close();
		}
		catch (Exception ex) {
			pst.close();
			throw ex;
		}
		
		return Collections.unmodifiableSet(cols);
    }

	public StatsTable findTable(String categoryText) {
		for (StatsTable t : plugin.MySql.getTables()) {
			if (categoryText.length() >= t.Category.length()) {
				if (categoryText.substring(0, t.Category.length()).equalsIgnoreCase(t.Category))
					return t;
			}
		}
		return null;
	}

	public String findTableField(StatsTable t, String fieldText) {
		fieldText = FieldUpdate.cleanFieldName(fieldText.trim());
		Set<String> flds = plugin.MySql.getFields(t);
		if (flds.contains(fieldText)) {
			return fieldText;
		}
		return null;
	}

	public boolean initSchema() {
		try {
	        getConn().prepareStatement("SELECT 1;").close();
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
				String playerIndex = t.hasPlayerName() ? " INDEX `by_player_name` (`player_name`), \n" : "";
				exec("CREATE TABLE IF NOT EXISTS `" + t.TableName + "` ( \n" +
					"  `uuid` VARCHAR(40) NOT NULL, \n" + 
					bungeeKey + 
					"  `created` BIGINT NOT NULL, \n" + 
					"  `updated` BIGINT NOT NULL, \n" + 
					playerName + playerIndex + pk + ");"
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
				trans.close();
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

	public Map<UUID, String> lookupPlayerNames(List<UUID> uuids) {
		StatsTable tbl = null;
		HashMap<UUID, String> map = new HashMap<UUID, String>();
		for (StatsTable t : tables) {
			if (t.hasPlayerName()) {
				tbl = t;
				break;
			}
		}
		if (tbl == null) {
			return map;
		}

		StringBuilder sbSelect = new StringBuilder();
		sbSelect.append("SELECT `uuid`, `player_name` FROM `");
		sbSelect.append(tbl.TableName);
		sbSelect.append("` WHERE `uuid` IN (");
		for (int ix = 0; ix < uuids.size(); ix++) {
			if (ix > 0) sbSelect.append(',');
			sbSelect.append('\'');
			sbSelect.append(uuids.get(ix));
			sbSelect.append('\'');
		}
		sbSelect.append(");");
		
        try (PreparedStatement pst = getConn().prepareStatement(sbSelect.toString())) {
			try (ResultSet rs = pst.executeQuery()) {
				if (rs != null) {
					while (rs.next()) {
						map.put(UUID.fromString(rs.getString(1)), rs.getString(2));
					}
					rs.close();
				}
			}
			pst.close();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		return map;
	}

	public List<PlayerStatsInfo> lookupPlayerByName(String partial) {
		ArrayList<PlayerStatsInfo> results = new ArrayList<PlayerStatsInfo>();
		StatsTable tbl = null;
		for (int ix = 0; ix < tables.size(); ix++) {
			if (tables.get(ix).hasPlayerName()) {
				tbl = tables.get(ix);
				break;
			}
		}
		
		if (tbl == null) {
			return results;
		}

		String preparedSql = "SELECT uuid, player_name FROM `" + tbl.TableName + "` "
				+ "WHERE player_name {where} "
				+ "GROUP BY uuid, player_name "
				+ "LIMIT 6;";
		
		PreparedStatement pst = null;
		try {
	        pst = getConn().prepareStatement(
	        		preparedSql.replace("{where}", "= '" + PlayerNameUpdate.cleanName(partial) + "'"));
			try (ResultSet rs = pst.executeQuery()) {
				while (rs != null && rs.next()) {
					results.add(new PlayerStatsInfo(plugin, UUID.fromString(rs.getString(1)), rs.getString(2)));
				}
				if (rs != null)
					rs.close();
			}
			pst.close();
			
			if (results.size() == 0) {
		        pst = getConn().prepareStatement(
		        		preparedSql.replace("{where}", "LIKE '" + PlayerNameUpdate.cleanName(partial) + "%'"));
    			try (ResultSet rs = pst.executeQuery()) {
					while (rs != null && rs.next()) {
						results.add(new PlayerStatsInfo(plugin, UUID.fromString(rs.getString(1)), rs.getString(2)));
					}
					if (rs != null)
						rs.close();
				}
				pst.close();
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
			if (pst != null) {
				try { pst.close(); } 
				catch (SQLException e) { }
			}
		}

		if (results.size() > 5) {
			results.clear();
		}

		return results;
	}

	public List<Map<String, Long>> fetchAllStats(UUID uniqueId, StatsTable tbl) {
		HashSet<String> ignore = new HashSet<String>();
		ignore.add("uuid");
		ignore.add("player_name");
		ignore.add("server");
		ignore.add("created");
		ignore.add("updated");
		List<Map<String, Long>> results = new ArrayList<Map<String, Long>>();
		PreparedStatement pst = null;
		try {
	        pst = getConn().prepareStatement("SELECT * FROM `" + tbl.TableName + "` "
					+ "WHERE uuid = '" + uniqueId + "';");

			try (ResultSet rs = pst.executeQuery()) {
				if (rs != null) {
					ResultSetMetaData rsmd = rs.getMetaData();
					int columnCount = rsmd.getColumnCount();
					while (rs.next()) {
						HashMap<String, Long> map = new HashMap<String,Long>();
						for (int ix = 1; ix <= columnCount; ix++) {
							String label = rsmd.getColumnLabel(ix).toLowerCase();
							if (ignore.contains(label)) {
								continue;
							}
							Object val = rs.getObject(ix);
							if (val instanceof Number) {
								map.put(label, ((Number)val).longValue());
							}
						}
						results.add(Collections.unmodifiableMap(map));
					}
					rs.close();
				}
			}
			pst.close();
		}
		catch(Exception ex) {
			ex.printStackTrace();
			if (pst != null) {
				try { pst.close(); } 
				catch (SQLException e) { }
			}
		}

		return results;
	}

	public List<NameValuePair<Long>> getTop(StatsTable tbl, String fieldName, int maxResults) throws Exception{
		
		ArrayList<NameValuePair<Long>> results = new ArrayList<NameValuePair<Long>>(); 

		try {
			String key = tbl.hasPlayerName() ? "player_name" : "uuid";
			String statement = ("SELECT `{key}`, {agg}(`{field}`) `{field}` "
					+ "FROM `{table}` "
					+ "GROUP BY uuid "
					+ "ORDER BY {agg}(`{field}`) DESC "
					+ "LIMIT 10;")
					.replace("{table}", tbl.TableName)
					.replace("{agg}", tbl.AggregateType())
					.replace("{key}", key)
					.replace("{field}", fieldName);
			
	        try (PreparedStatement pst = getConn().prepareStatement(statement)) {
				try (ResultSet rs = pst.executeQuery()) {
					if (rs != null) {
						while (rs.next()) {
							Long value = rs.getLong(2);
							if (value > 0L) {
								results.add(new NameValuePair<Long>(rs.getString(1), value));
							}
						}
						rs.close();
					}
				}
				pst.close();
	        }
	        
	        if (key == "uuid") {
	        	ArrayList<UUID> uuids = new ArrayList<UUID>();
	        	for (NameValuePair<Long> n : results) {
	        		uuids.add(UUID.fromString(n.Name));
	        	}
	        	Map<UUID, String> map = this.lookupPlayerNames(uuids);
	        	for (int ix = 0; ix < results.size(); ix++) {
	        		NameValuePair<Long> item = results.get(ix);
	        		item = new NameValuePair<Long>(map.get(UUID.fromString(item.Name)), item.Value);
	        		if (item.Name != null)
	        			results.set(ix, item);
	        	}
	        }
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		return results;
	}
}
