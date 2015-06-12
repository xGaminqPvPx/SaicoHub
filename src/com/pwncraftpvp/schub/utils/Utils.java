package com.pwncraftpvp.schub.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.pwncraftpvp.schub.core.Main;
import com.pwncraftpvp.schub.core.NPC;

public class Utils {
	
	private static Main main = Main.getInstance();
	private static final Random rand = new Random();
	
	/**
	 * Spawn a hub entity
	 * @param type - The entity type
	 * @param loc - The location
	 * @return The entity
	 */
	public static final LivingEntity spawnEntity(EntityType type, Location loc){
		LivingEntity entity = (LivingEntity) loc.getWorld().spawnEntity(loc, type);
		if(type == EntityType.MAGMA_CUBE){
			MagmaCube cube = (MagmaCube) entity;
			cube.setSize(4);
		}
		entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 20));
		entity.setRemoveWhenFarAway(false);
		return entity;
	}
	
	/**
	 * Set an npc location
	 * @param server - The server the npc represents
	 * @param loc - The location
	 */
	public static final void updateNpcLocation(String server, Location loc){
		if(main.servers.contains(server) == false){
			main.servers.add(server);
		}
		if(main.counts.containsKey(server) == false){
			main.counts.put(server, 0);
		}
		if(main.getConfig().getString("servers." + server + ".description") == null){
			main.getConfig().set("servers." + server + ".description", "&7Join the &6" + server + " &7Realm!");
			main.getConfig().set("servers." + server + ".maxslots", 250);
		}
		if(main.descs.containsKey(server) == false){
			main.descs.put(server, main.getConfig().getString("servers." + server + ".description").replace("&", "§"));
		}
		if(main.maxslots.containsKey(server) == false){
			main.maxslots.put(server, main.getConfig().getInt("servers." + server + ".maxslots"));
		}
		if(main.getConfig().getString("servers." + server + ".npc.world") == null){
			main.getConfig().set("servers." + server + ".npc.type", "villager");
			LivingEntity entity = Utils.spawnEntity(EntityType.VILLAGER, loc);
			NPC npc = new NPC(entity, server);
			main.npcs.put(server, npc);
			main.npcids.put(entity.getEntityId(), server);
			npc.updateHologram(false);
		}else{
			NPC npc = main.npcs.get(server);
			npc.getEntity().teleport(loc);
			npc.updateHologram(true);
		}
		main.getConfig().set("servers." + server + ".npc.x", loc.getX());
		main.getConfig().set("servers." + server + ".npc.y", loc.getY());
		main.getConfig().set("servers." + server + ".npc.z", loc.getZ());
		main.getConfig().set("servers." + server + ".npc.world", loc.getWorld().getName());
		main.saveConfig();
	}
	
	/**
	 * Get the npc location of a server
	 * @param server - The server to get the npc location of
	 * @return The npc location of the server
	 */
	public static final Location getNpcLocation(String server){
		if(main.getConfig().getString("servers." + server + ".npc.world") != null){
			double x,y,z;
			x = main.getConfig().getDouble("servers." + server + ".npc.x");
			y = main.getConfig().getDouble("servers." + server + ".npc.y");
			z = main.getConfig().getDouble("servers." + server + ".npc.z");
			World world = Bukkit.getWorld(main.getConfig().getString("servers." + server + ".npc.world"));
			return new Location(world, x, y, z);
		}else{
			return null;
		}
	}
	
	/**
	 * Get the server selector inventory
	 * @return The server selector inventory
	 */
	public static final Inventory getSelector(){
		main = Main.getInstance();
		int size = (main.getConfig().getInt("servers.selector.rows") * 9);
		Inventory inv = Bukkit.createInventory(null, size, ChatColor.DARK_GRAY + "Choose a Server!");
		for(int x = 0; x <= (size - 1); x++){
			ItemStack i = main.getConfig().getItemStack("servers.selector.inv." + x + ".item");
			if(i != null){
				inv.setItem(x, i);
			}
		}
		return inv;
	}
	
	/**
	 * Set the server selector inventory
	 * @param inv - The server selector inventory
	 */
	public static final void setSelector(Inventory inv){
		for(int x = 0; x <= (inv.getSize() - 1); x++){
			ItemStack i = inv.getItem(x);
			ItemStack confItem = main.getConfig().getItemStack("servers.selector.inv." + x + ".item");
			if(i != null){
				if(confItem == null || confItem.isSimilar(i) == false){
					main.getConfig().set("servers.selector.inv." + x + ".server", "none");
					main.getConfig().set("servers.selector.inv." + x + ".item", i);
				}
			}else{
				if(confItem != null){
					main.getConfig().set("servers.selector.inv." + x, null);
				}
			}
		}
		main.saveConfig();
	}
	
	/**
	 * Refresh all servers with the latest information
	 */
	public static final void refreshServers(){
		for(String s : main.servers){
			Utils.updatePlayerCount(s);
			if(main.npcs.containsKey(s) == true){
				NPC npc = main.npcs.get(s);
				Location loc = Utils.getNpcLocation(s);
				LivingEntity entity = npc.getEntity();
				if(entity.isDead() == true || entity.isValid() == false){
					npc.deleteHologram();
					entity = Utils.spawnEntity(entity.getType(), loc);
					npc = new NPC(entity, s);
					main.npcs.remove(s);
					if(main.npcids.containsKey(entity.getEntityId()) == true){
						main.npcids.remove(entity.getEntityId());
					}
					main.npcs.put(s, npc);
					main.npcids.put(entity.getEntityId(), s);
				}
				npc.updateHologram(false);
				if(entity.getLocation().distance(loc) > 0){
					entity.teleport(loc);
				}
			}
		}
		Utils.updatePlayerCount("ALL");
	}
	
	/**
	 * Connect a player to a server
	 * @param player - The player to move
	 * @param server - The new server
	 */
	public static final void connect(Player player, String server){
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(server);
		player.sendPluginMessage(main, "BungeeCord", out.toByteArray());
	}
	
	/**
	 * Update the player count for a server
	 * @param server - The server to update the count of
	 */
	public static final void updatePlayerCount(String server){
		Collection<? extends Player> players = Bukkit.getOnlinePlayers();
		if(players.size() > 0){
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("PlayerCount");
			out.writeUTF(server);
			Iterables.getFirst(players, null).sendPluginMessage(main, "BungeeCord", out.toByteArray());
		}
	}
	
	/**
	 * Remove all villagers on the server
	 */
	public static final void removeAllVillagers(){
		for(World w : Bukkit.getWorlds()){
			for(Entity e : w.getEntities()){
				if((e instanceof Player) == false){
					e.remove();
				}
			}
		}
	}
	
	/**
	 * Remove all holograms on the server
	 */
	public static final void removeAllHolograms(){
		for(Hologram hologram : HologramsAPI.getHolograms(main)){
            hologram.delete();
        }
	}
	
	/**
	 * Get a random chat color
	 * @return A random chat color
	 */
	public static final ChatColor getRandomColor(){
		ChatColor color = null;
		while(color == null){
			for(ChatColor c : ChatColor.values()){
				if(rand.nextDouble() <= 0.1){
					color = c;
					break;
				}
			}
		}
		return color;
	}
	
	/**
	 * Rename an itemstack
	 * @param item - The itemstack to rename
	 * @param name - The new name of the itemstack
	 * @param lore - The lore for the itemstack
	 * @return The renamed itemstack
	 */
	public static final ItemStack renameItem(ItemStack item, String name, String... lore){
	    ItemMeta meta = (ItemMeta) item.getItemMeta();
    	meta.setDisplayName(name);
    	List<String> desc = new ArrayList<String>();
    	for(int x = 0; x <= (lore.length - 1); x++){
    		desc.add(lore[x]);
    	}
	    meta.setLore(desc);
	    item.setItemMeta(meta);
	    return item;
	}
	
	/**
	 * Rename an itemstack
	 * @param item - The itemstack to rename
	 * @param name - The new name of the itemstack
	 * @return The renamed itemstack
	 */
	public static final ItemStack renameItem(ItemStack item, String name){
	    ItemMeta meta = (ItemMeta) item.getItemMeta();
    	meta.setDisplayName(name);
	    item.setItemMeta(meta);
	    return item;
	}

}
