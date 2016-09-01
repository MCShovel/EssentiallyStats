package com.steamcraftmc.EssentiallyStats.Commands;

import java.util.*;
import java.util.logging.Level;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.Controllers.PlayerStatsInfo;
import com.steamcraftmc.EssentiallyStats.Controllers.StatsTable;
import com.steamcraftmc.EssentiallyStats.tasks.ImportPlayerStats;
import com.steamcraftmc.EssentiallyStats.tasks.ShowPlayerStats;
import com.steamcraftmc.EssentiallyStats.tasks.ShowTopPlayerStats;
import com.steamcraftmc.EssentiallyStats.utils.FieldUpdate;

public class CmdStats extends BaseCommand implements TabCompleter {

	public CmdStats(MainPlugin plugin) {
		super(plugin, "stats", 0, 255);
	}

	@Override
	protected boolean doPlayerCommand(Player player, Command cmd, String commandLabel, String[] args) throws Exception {

		if (args.length == 0) {
			args = new String[] { player.getName() };
		}
		return doConsoleCommand(player, cmd, commandLabel, args);
	}

	@Override
	protected boolean doConsoleCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) throws Exception {
		if (args.length < 1)
			return false;

		String opOrPlayer = args[0].toLowerCase();
		if (opOrPlayer.equalsIgnoreCase("reload") && sender.hasPermission(super.permission + ".reload")) {
			plugin.reload();
			sender.sendMessage(plugin.Config.get("messages.reloaded", "&6Configuration reloaded."));
			return true;
		}
		if (opOrPlayer.equalsIgnoreCase("import") && sender.hasPermission(super.permission + ".import")) {
			new ImportPlayerStats(plugin, sender).runAsync(0);
			sender.sendMessage(plugin.Config.get("messages.import-start", "&6Import started."));
			return true;
		}

		StringBuilder sbcat = new StringBuilder();
		for (int ix = 1; ix < args.length; ix++) {
			if (sbcat.length() > 0)
				sbcat.append(' ');
			sbcat.append(args[ix]);
		}

		if (opOrPlayer.equalsIgnoreCase("top") && sender.hasPermission(super.permission + ".top")) {
			if (sbcat.length() == 0) {
				sender.sendMessage("Usage: /stats top (Category Name) (Field Name)");
				return true;
			}
			new ShowTopPlayerStats(plugin, sender, sbcat.toString())
				.runAsync(1);
		}
		else {
			PlayerStatsInfo psi = null;
			Player target = plugin.getServer().getPlayer(opOrPlayer);
			if (target != null) {
				psi = new PlayerStatsInfo(plugin, target);
			}
			new ShowPlayerStats(plugin, sender, opOrPlayer, sbcat.length() == 0 ? null : sbcat.toString(), psi)
				.runAsync(0);
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if ("stats".equalsIgnoreCase(cmd.getName().trim())) {
			plugin.log(Level.INFO, "'" + String.join("', '", args) + "'");
			List<String> result = new ArrayList<String>();
			try {
				if (args.length == 1) {
					for (PlayerStatsInfo p : plugin.MySql.lookupPlayerByName(args[0])) {
						result.add(p.name);
					}
				}
				else if (args.length > 1) {
					tabCompleteCategories(args, 1, result, args[0].equalsIgnoreCase("top"));
				}
			}
			catch(Exception ex) {
				ex.printStackTrace();
			}
			return result;
		} else{
			plugin.log(Level.INFO, "Not for me: '" + cmd.getName() + "'");
		}
		return null;
	}

	private void tabCompleteCategories(String[] args, int ixStart, List<String> result, boolean completeFields) {
		String fullText = "";
		for (int ix = ixStart; ix < args.length; ix++) {
			if (ix > ixStart) fullText += ' ';
			fullText += args[ix];
		}

		int split = 1 + fullText.lastIndexOf(' ');

		StatsTable match = null;
		for (StatsTable t : plugin.MySql.getTables()) {
			if (fullText.length() >= t.Category.length()) {
				if (fullText.substring(0, t.Category.length()).equalsIgnoreCase(t.Category))
					match = t;
			} else {
				if (t.Category.substring(0, fullText.length()).equalsIgnoreCase(fullText)) {
					result.add(t.Category.substring(split));
				}
			}
		}
		
		if (match != null && completeFields) {
			if (fullText.length() == match.Category.length()) {
				plugin.log(Level.INFO, "no space after category");
				return;
			}
			if (fullText.charAt(match.Category.length()) != ' ') {
				plugin.log(Level.INFO, "no space after category");
				return;
			}
			fullText = fullText.substring(match.Category.length() + 1);
			split = 1 + fullText.lastIndexOf(' ');
			plugin.log(Level.INFO, "looking for field '" + fullText + "'");
			fullText = FieldUpdate.cleanFieldName(fullText.trim());

			Set<String> flds = plugin.MySql.getFields(match);
			for (String fld : flds) {
				if (fld.startsWith(fullText)) {
					result.add(FieldUpdate.toDisplayName(fld).substring(split));
				}
			}
		}
	}
}
