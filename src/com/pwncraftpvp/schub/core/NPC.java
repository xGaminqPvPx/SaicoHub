package com.pwncraftpvp.schub.core;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;

public class NPC {
	
	private Main main = Main.getInstance();
	
	private LivingEntity entity;
	private String server;
	private Hologram hologram = null;
	public NPC(LivingEntity entity, String server){
		this.entity = entity;
		this.server = server;
	}
	
	/**
	 * Get the villager
	 * @return The villager
	 */
	public LivingEntity getEntity(){
		return entity;
	}
	
	/**
	 * Get the npc's location
	 * @return The npc's location
	 */
	public Location getLocation(){
		return entity.getLocation();
	}
	
	/**
	 * Update the npc's hologram
	 */
	public void updateHologram(boolean full){
		if(hologram == null || full == true){
			if(full == true){
				hologram.delete();
			}
			hologram = HologramsAPI.createHologram(main, entity.getLocation().add(0, 2.6, 0));
			hologram.appendTextLine(main.descs.get(server));
			int count = main.counts.get(server);
			if(count > 0){
				hologram.appendTextLine(main.playercount.replace("{players}", "" + main.counts.get(server)).replace("{maxslots}", "" + main.maxslots.get(server)));
			}else{
				hologram.appendTextLine(ChatColor.DARK_RED + "Offline");
			}
		}else{
			hologram.removeLine(1);
			int count = main.counts.get(server);
			if(count > 0){
				hologram.appendTextLine(main.playercount.replace("{players}", "" + main.counts.get(server)).replace("{maxslots}", "" + main.maxslots.get(server)));
			}else{
				hologram.appendTextLine(ChatColor.DARK_RED + "Offline");
			}
		}
	}
	
	/**
	 * Delete the npc's hologram
	 */
	public void deleteHologram(){
		hologram.delete();
	}

}
