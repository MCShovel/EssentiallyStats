package com.steamcraftmc.EssentiallyStats.Controllers;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.steamcraftmc.EssentiallyStats.MainPlugin;

public class PlayerRank {
	private final MainPlugin plugin;
	public final int Ordinal;
	public final String Name;
	public final String Description;
	private final String permission;
	private final String requires;
	private final String announce;
	private final String message;
	private final List<String> actions;
	private final Map<String, Long> objectives;

	public PlayerRank(MainPlugin plugin, String name, int ordinal, ConfigurationSection section) {
		this.plugin = plugin;
		this.Name = name;
		this.Ordinal = ordinal;
		this.Description = section.getString("description", "");
		this.permission = section.getString("permission", "permgroup." + name.toLowerCase());
		
	    String reqRank = section.getString("requires");
	    if (reqRank != null && reqRank.length() > 0) {
	    	this.requires = plugin.Config.get("ranks." + reqRank + ".permission", "!");
	    	if (this.requires == "!") {
	    		plugin.log(Level.SEVERE, "Unable to locate the required rank " + reqRank + " for rank " + Name);
	    	}
	    }
	    else { this.requires = null; }

	    this.announce = section.getString("announce", "");
	    this.message = section.getString("message", "");
	    this.actions = Collections.unmodifiableList(section.getStringList("actions"));
	    
	    
	    Map<String, Long> lmap = new HashMap<String,Long>();
	    ConfigurationSection cfg = plugin.Config.getSection(section.getCurrentPath() + ".objectives");
	    for (String k : cfg.getKeys(false)) {
	    	lmap.put(k, cfg.getLong(k));
	    }
	    this.objectives = Collections.unmodifiableMap(lmap);
	}
	
	public boolean hasRank(Player player) {
		return player != null && player.hasPermission(this.permission);
	}
	
	public static List<PlayerRank> getNextRanks(MainPlugin plugin, Player player) {
		ArrayList<PlayerRank> ranks = new ArrayList<PlayerRank>();
		for(PlayerRank rank : plugin.Config.getRanks().values()) {
			if (rank.canAchieveRank(player)) {
				ranks.add(rank);
			}
		}
		return Collections.unmodifiableList(ranks);
	}

	public boolean hasPrerequisites(Player player) {
		if (player == null || this.requires == "!")
			return false; //error
		
		boolean canAchieve = this.requires == null || this.requires.length() == 0 ? true 
				: player.hasPermission(this.requires);
		return canAchieve;
	}

	public boolean canAchieveRank(Player player) {
		return hasPrerequisites(player) && !hasRank(player);
	}

	public CheckResults checkRequirements(Player player) {
		CheckResults results = new CheckResults(new PlayerStatsInfo(plugin, player), this);
		
		Map<String, PlayerObjective> all = plugin.Config.getObjectives();
		for (Entry<String, Long> o : this.objectives.entrySet()) {
			PlayerObjective ob = all.get(o.getKey().toLowerCase());
			boolean success = false;
			String message;
			long current = 0;
			if (ob == null) {
				plugin.log(Level.SEVERE, "Unable to locate objective " + o.getKey());
				success = false;
				message = plugin.Config.ConfigurationError();
				current = 0;
			}
			else {
				current = ob.currentValue(player);
				success = current > o.getValue();
				message = success
						? ob.formatComplete(player, current, o.getValue())
						: ob.formatIncomplete(player, current, o.getValue());
			}
			results.addResult(success, current, o.getValue(), message);
		}
		return results;
	}

	public void execForPlayer(Player player) {
    	boolean dropped = false;
    	
    	List<String> actions = this.actions;
    	if (actions == null || actions.size() == 0) {
    		return;
    	}
    	
    	for (int ix = 0; ix < actions.size(); ix++) {
    		String cmdText = formatForPlayer(player, actions.get(ix));
    		plugin.log(Level.INFO, "Execute: " + cmdText);
    		dropped |= !runCommand(cmdText);
    	}

    	if (dropped) {
			String msg = plugin.Config.get("formatting.inventoryFull", "&cYou inventory was full, some items are on the ground.");
	    	player.sendMessage(msg);
		}
    	
    	if (this.message != null && this.message.length() > 0) {
    		String msg = ChatColor.translateAlternateColorCodes('&', this.message);
    		msg = msg.replace("{name}", player.getDisplayName());
    		msg = msg.replace("{rank}", this.Name);
    		player.sendMessage(msg);
    	}

    	if (this.announce != null && this.announce.length() > 0) {
    		String msg = ChatColor.translateAlternateColorCodes('&', this.announce);
    		msg = msg.replace("{name}", player.getDisplayName());
    		msg = msg.replace("{rank}", this.Name);
    		plugin.Broadcast.sendMessage(player, "", msg);
    	}
	}
	
	@SuppressWarnings("deprecation")
	private boolean runCommand(String cmdText) {
		if (cmdText.startsWith("/")) {
			cmdText = cmdText.substring(1);
		}
		
    	String[] args = cmdText.split("\\s+", 6);
    	if (args.length > 2 && args[0].equalsIgnoreCase("give")) {
			Player player = Bukkit.getPlayer(args[1]);
			if (player != null) {
	            Material material = Material.matchMaterial(args[2]);
	
	            if (material == null) {
	                material = Bukkit.getUnsafe().getMaterialFromInternalName(args[2]);
	            }
	
	            if (material != null) {
	                int amount = 1;
	                short data = 0;
	
	                if (args.length > 3) {
	                	try {
	                    amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
	                	}
                        catch (NumberFormatException e) { 
	                    	plugin.log(Level.SEVERE, "Invalid quantity: " + cmdText);
	                    	amount = 1; 
                    	}
	                }
	            	
                    if (args.length > 4) {
                        try {
                            data = Short.parseShort(args[4]);
                        } 
                        catch (NumberFormatException e) { 
	                    	plugin.log(Level.SEVERE, "Invalid data: " + cmdText);
                        	data = 0; 
                    	}
                    }
	
	                ItemStack stack = new ItemStack(material, amount, data);
	
	                if (args.length > 5) {
	                    try {
	                        stack = Bukkit.getUnsafe().modifyItemStack(stack, args[5]);
	                    } catch (Throwable t) {
	                    	plugin.log(Level.SEVERE, "Invalid tag: " + cmdText);
	                    }
	                }
	
	                PlayerInventory inv = player.getInventory();
	                int emptyIx = inv.firstEmpty();
	                if (emptyIx >= 0) {
	                	inv.setItem(emptyIx, stack);
		                return true;
	                }

                	player.getWorld().dropItemNaturally(player.getLocation(), stack);
	                return false;
	            }
	            else {
                	plugin.log(Level.SEVERE, "Unknown material: " + args[2]);
	            }
			}
    	}
    	
    	Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmdText);
        return true;
	}

	private String formatForPlayer(Player player, String text) {
    	if (text == null) {
    		return null;
    	}
		text = text.replace("@p", player.getName());
		Block loc = player.getLocation().getBlock(); 
		text = text.replace("@x", String.valueOf(loc.getX()));
		text = text.replace("@y", String.valueOf(loc.getY()));
		text = text.replace("@z", String.valueOf(loc.getZ()));
		text = ChatColor.translateAlternateColorCodes('&', text);
		return text;
	}
}
