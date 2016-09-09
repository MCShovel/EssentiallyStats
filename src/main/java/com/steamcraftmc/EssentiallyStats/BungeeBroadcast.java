package com.steamcraftmc.EssentiallyStats;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class BungeeBroadcast implements PluginMessageListener {
	
	private MainPlugin plugin;
	private boolean bungeecord;

	public BungeeBroadcast(MainPlugin plugin) {
		this.plugin = plugin;
	}

	public void start() {
		bungeecord = plugin.Config.getBoolean("settings.bungeecord", true);
		if (bungeecord) {
	        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
	        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
		}
	}

	public void stop() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, "BungeeCord");
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, "BungeeCord", this);		
	}
	
	public void sendMessage(Player except, String permission, String message) {
		if (permission == null) {
			permission = "";
		}
		if (message == null || message.length() == 0) {
			return;
		}
		
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			if (!p.equals(except) && (permission.length() == 0 || p.hasPermission(permission))) {
				p.sendMessage(message);
			}
		}
		
		if (bungeecord) {
	        ByteArrayDataOutput out = ByteStreams.newDataOutput();
	        
	        out.writeUTF("Forward");
	        out.writeUTF("ONLINE");
	        out.writeUTF("HACX");

	        ByteArrayDataOutput msg = ByteStreams.newDataOutput();
	        msg.writeUTF(permission + ":" + message);
	        out.writeShort(msg.toByteArray().length);
	        out.write(msg.toByteArray());
			for (Player p : plugin.getServer().getOnlinePlayers()) {
	        	p.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
	        	break;
			}
		}
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("BungeeCord")) {
			return;
		}
		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String subchannel = in.readUTF();

		if (subchannel.equals("HACX")) {
			int size = in.readShort();
			byte[] msgbytes = new byte[size];
			in.readFully(msgbytes);
			ByteArrayDataInput msg = ByteStreams.newDataInput(msgbytes);
			String cmd = msg.readUTF();
			int offset = cmd.indexOf(':');
			if (offset >= 0) {
		        String permission = cmd.substring(0, offset);
		        String txtMessage = cmd.substring(1 + offset);

				for (Player p : plugin.getServer().getOnlinePlayers()) {
					if (permission.length() == 0 || p.hasPermission(permission)) {
						p.sendMessage(txtMessage);
					}
				}
			}
		}
	}
}
