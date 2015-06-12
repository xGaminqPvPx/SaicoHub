package com.pwncraftpvp.schub.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.pwncraftpvp.schub.tasks.HideCooldownTask;
import com.pwncraftpvp.schub.tasks.JoinCooldownTask;
import com.pwncraftpvp.schub.utils.TextUtils;
import com.pwncraftpvp.schub.utils.Utils;

public class Main extends JavaPlugin {
	
	private static Main instance;
	private String yellow = ChatColor.YELLOW + "";
	private String gray = ChatColor.GRAY + "";
	
	public int totalplayers;
	public String boardtitle;
	public String playercount;
	public boolean shopenabled;
	public Inventory serverselector;
	public ItemStack saicoshop = Utils.renameItem(new ItemStack(Material.CHEST), ChatColor.GREEN + "Saico Shop", ChatColor.GRAY + "View the shop.");
	public ItemStack selector = Utils.renameItem(new ItemStack(Material.COMPASS), ChatColor.GOLD + "Server Selector", ChatColor.GRAY + "Manually choose a server to join.");
	public ItemStack playersOn = Utils.renameItem(new ItemStack(Material.REDSTONE_TORCH_ON), ChatColor.GOLD + "Player Visibility " + ChatColor.DARK_GRAY + TextUtils.getDoubleArrow() + ChatColor.GREEN + " On", 
			ChatColor.GRAY + "Right click to disable players.");
	public ItemStack playersOff = Utils.renameItem(new ItemStack(Material.REDSTONE_TORCH_ON), ChatColor.GOLD + "Player Visibility " + ChatColor.DARK_GRAY + TextUtils.getDoubleArrow() + ChatColor.RED + " Off", 
			ChatColor.GRAY + "Right click to enable players");
	
	public List<String> editing = new ArrayList<String>();
	public List<String> servers = new ArrayList<String>();
	public List<String> boardlines = new ArrayList<String>();
	public List<String> visibility = new ArrayList<String>();
	public List<String> secondjump = new ArrayList<String>();
	public List<ChatColor> colors = Arrays.asList(ChatColor.RED, ChatColor.BLUE, ChatColor.GREEN, 
			ChatColor.GOLD, ChatColor.YELLOW, ChatColor.BLACK, ChatColor.AQUA, ChatColor.DARK_GRAY);
	
	public HashMap<String, NPC> npcs = new HashMap<String, NPC>();
	public HashMap<String, String> descs = new HashMap<String, String>();
	public HashMap<Integer, String> npcids = new HashMap<Integer, String>();
	public HashMap<String, Integer> counts = new HashMap<String, Integer>();
	public HashMap<String, Integer> maxslots = new HashMap<String, Integer>();
	public HashMap<String, JoinCooldownTask> joincooldown = new HashMap<String, JoinCooldownTask>();
	public HashMap<String, HideCooldownTask> hidecooldown = new HashMap<String, HideCooldownTask>();
	
	/**
	 * Get the instance of this class
	 * @return The instance of this class
	 */
	public static final Main getInstance(){
		return instance;
	}
	
	public void onEnable(){
		instance = this;
		
		Utils.removeAllVillagers();
		
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new Events());
		this.getServer().getPluginManager().registerEvents(new Events(), this);
		
		File file = new File(this.getDataFolder(), "config.yml");
		if(file.exists() == false){
			this.getConfig().set("settings.saicoshop", false);
			this.getConfig().set("scoreboard.title", "&aSaico&fPvP");
			this.getConfig().set("scoreboard.lines." + 1, "&6Total Players: &7{total}");
			this.getConfig().set("scoreboard.lines." + 2, "&6SaicoPoints: &7{saicopoints}");
			this.getConfig().set("scoreboard.lines." + 3, "{blank}");
			this.getConfig().set("scoreboard.lines." + 1, "&6Skeleton: &7{HHSkeleton}");
			this.getConfig().set("scoreboard.lines." + 4, "&f&lplay.&a&lsaicopvp&f&l.com");
			this.getConfig().set("servers.playercount", "&7Players: &6{players}/{maxslots}");
			this.getConfig().set("servers.skeleton.description", "&7Join the &6Skeleton &7Realm");
			this.getConfig().set("servers.skeleton.maxslots", 250);
			this.getConfig().set("servers.skeleton.npc.type", "skeleton");
			this.getConfig().set("servers.selector.rows", 1);
			this.saveConfig();
		}else{
			if(this.getConfig().getBoolean("settings.saicoshop") == false){
				this.getConfig().set("settings.saicoshop", false);
			}
			this.saveConfig();
		}
		
		playercount = this.getConfig().getString("servers.playercount").replace("&", "§");
		boardtitle = this.getConfig().getString("scoreboard.title").replace("&", "§");
		shopenabled = this.getConfig().getBoolean("settings.saicoshop");
		serverselector = Utils.getSelector();
		
		int color = 0;
		for(int x = 1; x <= this.getConfig().getConfigurationSection("scoreboard.lines").getKeys(false).size(); x++){
			String line = this.getConfig().getString("scoreboard.lines." + x);
			if(line != null){
				if(line.equalsIgnoreCase("{blank}") == false){
					boardlines.add(line.replace("&", "§"));
				}else{
					boardlines.add(colors.get(color) + "");
					color++;
				}
			}
		}
		
		for(String s : this.getConfig().getConfigurationSection("servers").getKeys(false)){
			if(s.equalsIgnoreCase("playercount") == false && s.equalsIgnoreCase("selector") == false){
				servers.add(s);
				counts.put(s, 0);
				maxslots.put(s, this.getConfig().getInt("servers." + s + ".maxslots"));
				descs.put(s, this.getConfig().getString("servers." + s + ".description").replace("&", "§"));
				Location loc = Utils.getNpcLocation(s);
				if(loc != null){
					EntityType type = EntityType.VILLAGER;
					if(this.getConfig().getString("servers." + s + ".npc.type") != null){
						type = EntityType.valueOf(this.getConfig().getString("servers." + s + ".npc.type").toUpperCase());
					}else{
						this.getConfig().set("servers." + s + ".npc.type", "villager");
						this.saveConfig();
					}
					LivingEntity entity = Utils.spawnEntity(type, loc);
					NPC npc = new NPC(entity, s);
					npc.updateHologram(false);
					npcs.put(s, npc);
					npcids.put(entity.getEntityId(), s);
				}
			}
		}
		
		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
			public void run(){
				Utils.refreshServers();
				for(Player p : Bukkit.getOnlinePlayers()){
					SPlayer sp = new SPlayer(p);
					sp.setScoreboard();
				}
			}
		}, 0, 100);
	}
	
	public void onDisable(){
		Utils.removeAllHolograms();
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(sender instanceof Player){
			Player player = (Player) sender;
			SPlayer splayer = new SPlayer(player);
			if(cmd.getName().equalsIgnoreCase("saicohub")){
				if(player.hasPermission("saicohub.commands") == true || player.getName().equalsIgnoreCase("JjPwN1")){
					if(args.length > 0){
						if(args[0].equalsIgnoreCase("setnpc")){
							if(args.length == 2){
								String server = args[1];
								Utils.updateNpcLocation(server, player.getLocation());
								splayer.sendMessage("You have set the npc for " + gray + server + yellow + ".");
							}else{
								splayer.sendError("Usage: /" + cmd.getName() + " setnpc <server>");
							}
						}else if(args[0].equalsIgnoreCase("editselector")){
							if(editing.contains(player.getName()) == true){
								editing.remove(player.getName());
							}
							editing.add(player.getName());
							player.openInventory(serverselector);
							splayer.sendMessage("You are now editing the server selector.");
						}else if(args[0].equalsIgnoreCase("reload")){
							this.reloadConfig();
							this.saveConfig();
							Utils.removeAllVillagers();
							Utils.removeAllHolograms();
							servers.clear();
							maxslots.clear();
							descs.clear();
							boardlines.clear();
							npcs.clear();
							npcids.clear();
							for(String s : this.getConfig().getConfigurationSection("servers").getKeys(false)){
								if(s.equalsIgnoreCase("playercount") == false && s.equalsIgnoreCase("selector") == false){
									servers.add(s);
									maxslots.put(s, this.getConfig().getInt("servers." + s + ".maxslots"));
									descs.put(s, this.getConfig().getString("servers." + s + ".description").replace("&", "§"));
									Location loc = Utils.getNpcLocation(s);
									if(loc != null){
										EntityType type = EntityType.VILLAGER;
										if(this.getConfig().getString("servers." + s + ".npc.type") != null){
											type = EntityType.valueOf(this.getConfig().getString("servers." + s + ".npc.type").toUpperCase());
										}
										LivingEntity entity = Utils.spawnEntity(type, loc);
										NPC npc = new NPC(entity, s);
										npc.updateHologram(false);
										npcs.put(s, npc);
										npcids.put(entity.getEntityId(), s);
									}
								}
							}
							int color = 0;
							for(int x = 1; x <= this.getConfig().getConfigurationSection("scoreboard.lines").getKeys(false).size(); x++){
								String line = this.getConfig().getString("scoreboard.lines." + x);
								if(line != null){
									if(line.equalsIgnoreCase("{blank}") == false){
										boardlines.add(line.replace("&", "§"));
									}else{
										boardlines.add(colors.get(color) + "");
										color++;
									}
								}
							}
							playercount = this.getConfig().getString("servers.playercount").replace("&", "§");
							boardtitle = this.getConfig().getString("scoreboard.title").replace("&", "§");
							shopenabled = this.getConfig().getBoolean("settings.saicoshop");
							splayer.sendMessage("You have reloaded the configuration.");
						}else if(args[0].equalsIgnoreCase("help")){
							splayer.sendHelp();
						}else{
							splayer.sendHelp();
						}
					}else{
						splayer.sendHelp();
					}
				}else{
					splayer.sendError("You do not have permission to do this.");
				}
			}
		}
		return false;
	}

}
