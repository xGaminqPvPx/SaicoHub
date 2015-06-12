package com.pwncraftpvp.schub.core;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.pwncraftpvp.schub.tasks.JoinCooldownTask;
import com.pwncraftpvp.schub.utils.TextUtils;
import com.pwncraftpvp.schub.utils.Utils;

public class SPlayer {
	
	private Main main = Main.getInstance();
	private String yellow = ChatColor.YELLOW + "";
	private String gray = ChatColor.GRAY + "";
	
	private Player player;
	public SPlayer(Player player){
		this.player = player;
	}
	
	/**
	 * Send a message header to the player
	 * @param header - The header to be sent
	 */
	public void sendMessageHeader(String header){
		player.sendMessage(TextUtils.centerText(yellow + "-=-(" + gray + TextUtils.getDoubleArrow() + yellow + ")-=-" + "  " + gray + header + "  " + yellow + "-=-(" + gray + TextUtils.getBackwardsDoubleArrow()
				+ yellow + ")-=-"));
	}
	
	/**
	 * Send a message to the player
	 * @param message - The message to be sent
	 */
	public void sendMessage(String message){
		player.sendMessage(yellow + message);
	}
	
	/**
	 * Send an error message to the player
	 * @param error - The error message to be sent
	 */
	public void sendError(String error){
		player.sendMessage(ChatColor.DARK_RED + error);
	}
	
	/**
	 * Send help to the player
	 */
	public void sendHelp(){
		this.sendMessageHeader("SaicoHub v" + main.getDescription().getVersion());
		player.sendMessage(gray + "/saicohub reload");
		player.sendMessage(yellow + "  Reload the configuration.");
		player.sendMessage(gray + "/saicohub editselector");
		player.sendMessage(yellow + "  Modify the server selector.");
		player.sendMessage(gray + "/saicohub setnpc <server>");
		player.sendMessage(yellow + "  Set the npc location for a server.");
	}
	
	/**
	 * Set the player's inventory
	 */
	public void setInventory(boolean visibility){
		player.getInventory().clear();
		ItemStack toggled = main.playersOn;
		if(visibility == false){
			toggled = main.playersOff;
		}
		if(main.shopenabled == true){
			player.getInventory().setItem(1, main.selector);
			player.getInventory().setItem(4, main.saicoshop);
			player.getInventory().setItem(7, toggled);
		}else{
			player.getInventory().setItem(3, main.selector);
			player.getInventory().setItem(5, toggled);
		}
	}
	
	/**
	 * Set the player's scoreboard
	 */
	@SuppressWarnings("deprecation")
	public void setScoreboard(){
		if(main.boardtitle.length() <= 32){
			Scoreboard board = player.getScoreboard();
			if(board == null){
				board = Bukkit.getScoreboardManager().getNewScoreboard();
				player.setScoreboard(board);
			}
			
			Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
			if(obj == null){
				obj = board.registerNewObjective("board", "dummy");
				obj.setDisplayName(main.boardtitle);
				obj.setDisplaySlot(DisplaySlot.SIDEBAR);
			}else{
				obj.setDisplayName(main.boardtitle);
			}
			
			com.pwncraftpvp.scglobal.core.SPlayer splayer = new com.pwncraftpvp.scglobal.core.SPlayer(player);
			
			int score = main.boardlines.size();
			for(String s : main.boardlines){
				s = s.replace("{total}", "" + main.totalplayers).replace("{saicopoints}", "" + splayer.getSaicoPoints());
				for(String e : main.servers){
					s = s.replace("{" + e.toLowerCase() + "}", "" + main.counts.get(e));
				}
				
				String substring = s;
				if(substring.length() > 16){
					substring = s.substring(0, 16);
				}else if(substring.length() >= 3){
					substring = s.substring(0, 3);
				}
				if(board.getTeam(substring) == null){
					board.registerNewTeam(substring);
				}
				Team team = board.getTeam(substring);
				OfflinePlayer op = Bukkit.getOfflinePlayer(substring);
				if(team.hasPlayer(op) == false){
					team.addPlayer(op);
				}
				if(s.length() > 16){
					team.setSuffix(s.substring(16, s.length()));
				}else if(substring.length() >= 3){
					team.setSuffix(s.substring(3, s.length()));
				}
				obj.getScore(op).setScore(score);
				score--;
			}
		}else{
			main.getLogger().info("The scoreboard title you entered in the configuration exceeds Minecraft's limit of 32 characters.");
		}
	}
	
	/**
	 * Attempt to connect the player to a server
	 * @param server - The server
	 */
	public void attemptToConnect(String server){
		if(main.joincooldown.containsKey(player.getName()) == false){
			if(com.pwncraftpvp.scglobal.utils.Utils.getBannedServer(player.getName(), server) == null){
				Utils.connect(player, server);
			}else{
				this.sendError("You are banned on this server!");
			}
			JoinCooldownTask task = new JoinCooldownTask(player);
			task.runTaskTimer(main, 0, 20);
			main.joincooldown.put(player.getName(), task);
		}else{
			this.sendError("Please wait " + main.joincooldown.get(player.getName()).getTimeLeft() + " seconds before attempting to join again.");
		}
	}

}
