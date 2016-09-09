package com.steamcraftmc.EssentiallyStats.Commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.Controllers.CheckResults;
import com.steamcraftmc.EssentiallyStats.Controllers.PlayerRank;
import com.steamcraftmc.EssentiallyStats.Controllers.PlayerStatsInfo;

public class CmdRank  extends BaseCommand implements TabCompleter {

	public CmdRank(MainPlugin plugin) {
		super(plugin, "rank", 0, 2);
	}

	@Override
	protected boolean doPlayerCommand(Player player, Command cmd, String commandLabel, String[] args) throws Exception {
		if (args.length == 0) {
			args = new String[] { "check" };
		}
		if (args[0].equalsIgnoreCase("check")) {
			doRankCheck(player, args.length > 1 ? args[1] : "");
			return true;
		}
		if (args[0].equalsIgnoreCase("list") && player.hasPermission(super.permission + ".list")) {
			doRankList(player);
			return true;
		}
		if (args[0].equalsIgnoreCase("reload") && player.hasPermission(super.permission + ".reload")) {
			plugin.reload();
			player.sendMessage(plugin.Config.get("messages.reloaded", "&6Configuration reloaded."));
			return true;
		}
		
		
		
		return false;
	}
	
	
	private void doRankList(Player player) {
		ArrayList<PlayerRank> ranks = new ArrayList<PlayerRank>();
		ranks.addAll(plugin.Config.getRanks().values());
		ranks.sort(new Comparator<PlayerRank>() {
			@Override
			public int compare(PlayerRank o1, PlayerRank o2) {
				return Integer.compare(o1.Ordinal, o2.Ordinal);
			}});
		
		StringBuilder text = new StringBuilder();
		text.append(plugin.Config.getTitle("Ranks"));
		
		for (PlayerRank rank : ranks) {
			text.append("\n");
			text.append(rank.hasRank(player) ? '*' : '-');
			text.append(plugin.Config.format("messages.rank-desc-format", "&6{name}: &7{desc}", 
					"name", rank.Name, "desc", rank.Description));
		}
		
		player.sendMessage(text.toString());
	}
	
	
	private void doRankCheck(Player player, String userName) {
		if (userName == null || userName.length() == 0) {
			userName = player.getName(); 
		}
		if (!userName.equalsIgnoreCase(player.getName()) && !player.hasPermission(super.permission + ".others")) {
			userName = player.getName();
		}

		List<PlayerStatsInfo> found = plugin.MySql.lookupPlayerByName(userName);
		if (found.size() != 1) {
			player.sendMessage(plugin.Config.PlayerNotFound(userName));
			return;
		}
		PlayerStatsInfo psi = found.get(0);
		Player user = plugin.getServer().getPlayer(psi.uniqueId);
		if (user == null) {
			player.sendMessage(plugin.Config.PlayerNotOnline(psi.name));
			return;
		}

		List<PlayerRank> ranks = PlayerRank.getNextRanks(plugin, user);
		
		if (ranks.size() == 0) {
			player.sendMessage(plugin.Config.get("messages.no-more-ranks", "&6You are at the highest rank."));
			return;
		}
		if (ranks.size() > 1) {
			player.sendMessage(plugin.Config.ConfigurationError());
			return;
		}
		
		CheckResults result = ranks.get(0).checkRequirements(user);
		
		StringBuilder text = new StringBuilder();
		text.append(plugin.Config.getTitle(result.getRankName() + " Progress"));
		for (CheckResults.PartialResult r : result.getResults()) {
			text.append('\n');
			text.append(r.Message);
		};
		player.sendMessage(text.toString());
		
		if (player.getUniqueId() == user.getUniqueId()) {
			if (result.isComplete()) {
				result.complete(user);
			}
		}
	}


	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		ArrayList<String> result = new ArrayList<String>();
		
		if (args.length == 0 || args[0].length() == 0) {
			result.add("check");
			result.add("list");
			if (sender.hasPermission("essentials.rank.reload"))
				result.add("reload");
		}
		else if (args[0].equalsIgnoreCase("check") && args.length > 1) {
			if (args[1].length() >= 2) {
				for (PlayerStatsInfo p : plugin.MySql.lookupPlayerByName(args[1])) {
					result.add(p.name);
				}
			}
		}
		
		return result;
	}
}
