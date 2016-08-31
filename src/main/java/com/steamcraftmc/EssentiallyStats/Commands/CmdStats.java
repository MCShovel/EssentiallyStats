package com.steamcraftmc.EssentiallyStats.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.steamcraftmc.EssentiallyStats.MainPlugin;
import com.steamcraftmc.EssentiallyStats.Controllers.PlayerStatsInfo;
import com.steamcraftmc.EssentiallyStats.tasks.ImportPlayerStats;
import com.steamcraftmc.EssentiallyStats.tasks.ShowPlayerStats;

public class CmdStats extends BaseCommand {

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
		if (opOrPlayer.equals("reload") && sender.hasPermission(super.permission + ".reload")) {
			plugin.reload();
			sender.sendMessage(plugin.Config.get("messages.reloaded", "&6Configuration reloaded."));
			return true;
		}
		if (opOrPlayer.equals("import") && sender.hasPermission(super.permission + ".import")) {
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

		PlayerStatsInfo psi = null;
		Player target = plugin.getServer().getPlayer(opOrPlayer);
		if (target != null) {
			psi = new PlayerStatsInfo(plugin, target);
		}

		new ShowPlayerStats(plugin, sender, opOrPlayer, sbcat.length() == 0 ? null : sbcat.toString(), psi)
			.runAsync(0);
		return true;
	}
}
