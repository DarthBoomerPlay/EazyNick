package net.dev.nickplugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.dev.nickplugin.utils.LanguageFileUtils;
import net.dev.nickplugin.utils.NickManager;
import net.dev.nickplugin.utils.Utils;

public class UnnickCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(sender instanceof Player) {
			Player p = (Player) sender;
			
			if(p.hasPermission("nick.use") || Utils.hasLuckPermsPermission(p.getUniqueId(), "nick.use")) {
				NickManager api = new NickManager(p);
				
				if((Utils.canUseNick.get(p.getUniqueId()))) {
					if(Utils.nickedPlayers.contains(p.getUniqueId())) {
						api.unnickPlayer();
						
						p.sendMessage(Utils.PREFIX + ChatColor.translateAlternateColorCodes('&', LanguageFileUtils.cfg.getString("Messages.Unnick")));
					} else {
						p.sendMessage(Utils.PREFIX + ChatColor.translateAlternateColorCodes('&', LanguageFileUtils.cfg.getString("Messages.NotNicked")));
					}
				} else {
					p.sendMessage(Utils.PREFIX + ChatColor.translateAlternateColorCodes('&', LanguageFileUtils.cfg.getString("Messages.NickDelay")));
				}
			} else {
				p.sendMessage(Utils.NO_PERM);
			}
		} else {
			Utils.sendConsole(Utils.NOT_PLAYER);
		}
		
		return true;
	}

}