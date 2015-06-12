package com.pwncraftpvp.schub.tasks;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.pwncraftpvp.schub.core.Main;

public class HideCooldownTask extends BukkitRunnable {
	
	private Main main = Main.getInstance();
	
	private Player player;
	public HideCooldownTask(Player player){
		this.player = player;
	}
	
	private int time = 6;
	private int runtime = 0;
	
	public void run(){
		int timeleft = (time - runtime);
		if(timeleft > 1){
			runtime++;
		}else{
			this.cancelTask();
		}
	}
	
	/**
	 * Perform necessary functions to cancel the task
	 */
	public void cancelTask(){
		if(main.hidecooldown.containsKey(player.getName()) == true){
			main.hidecooldown.remove(player.getName());
		}
		this.cancel();
	}
	
	/**
	 * Get the time left
	 * @return The time left
	 */
	public int getTimeLeft(){
		return (time - runtime);
	}

}
