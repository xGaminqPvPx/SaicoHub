package com.pwncraftpvp.schub.core;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.util.Vector;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.pwncraftpvp.schub.tasks.HideCooldownTask;
import com.pwncraftpvp.schub.utils.ParticleEffect;
import com.pwncraftpvp.schub.utils.ParticleUtils;
import com.pwncraftpvp.schub.utils.Utils;

public class Events implements Listener,PluginMessageListener {
	
	private Main main = Main.getInstance();
	
	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message){
		if(channel.equalsIgnoreCase("BungeeCord") == true){
			ByteArrayDataInput in = ByteStreams.newDataInput(message);
			String subchannel = in.readUTF();
			if(subchannel.equalsIgnoreCase("PlayerCount") == true){
				try{
					String server = in.readUTF();
					int players = in.readInt();
					if(server.equalsIgnoreCase("all") == false){
						if(main.counts.containsKey(server) == true){
							main.counts.remove(server);
						}
						main.counts.put(server, players);
					}else{
						main.totalplayers = players;
					}
				}catch (Exception ex){
					main.getLogger().info("An error occured while getting a server's player count. This is likely due to an incorrect server name in the config.");
				}
			}
		}
	}
	
	@EventHandler
	public void playerJoin(final PlayerJoinEvent event){
		Player player = event.getPlayer();
		SPlayer splayer = new SPlayer(player);
		player.setAllowFlight(true);
		player.setFoodLevel(20);
		player.setHealth(20);
		splayer.setScoreboard();
		splayer.setInventory(true);
		for(Player p : Bukkit.getOnlinePlayers()){
			if(main.visibility.contains(p.getName()) == true){
				p.hidePlayer(player);
			}
		}
	}
	
	@EventHandler
	public void inventoryClose(InventoryCloseEvent event){
		if(event.getPlayer() instanceof Player){
			Player player = (Player) event.getPlayer();
			if(main.editing.contains(player.getName()) == true && event.getInventory().getTitle().contains("Choose") == true){
				Utils.setSelector(event.getInventory());
				main.serverselector = Utils.getSelector();
				main.editing.remove(player.getName());
			}
		}
	}
	
	@EventHandler
	public void playerInteract(PlayerInteractEvent event){
		Player player = event.getPlayer();
		SPlayer splayer = new SPlayer(player);
		if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK){
			ItemStack item = event.getItem();
			if(item != null){
				if(item.getType() == Material.COMPASS){
					event.setCancelled(true);
					player.openInventory(main.serverselector);
				}else if(item.getType() == Material.CHEST){
					event.setCancelled(true);
					player.chat("/points");
				}else if(item.getType() == Material.REDSTONE_TORCH_ON){
					event.setCancelled(true);
					if(main.hidecooldown.containsKey(player.getName()) == false){
						if(main.visibility.contains(player.getName()) == false){
							for(Player p : Bukkit.getOnlinePlayers()){
								player.hidePlayer(p);
							}
							splayer.setInventory(false);
							main.visibility.add(player.getName());
						}else{
							for(Player p : Bukkit.getOnlinePlayers()){
								player.showPlayer(p);
							}
							splayer.setInventory(true);
							main.visibility.remove(player.getName());
						}
						HideCooldownTask task = new HideCooldownTask(player);
						task.runTaskTimer(main, 0, 20);
						main.hidecooldown.put(player.getName(), task);
					}else{
						splayer.sendError("Please wait " + main.hidecooldown.get(player.getName()).getTimeLeft() + " seconds before using this again.");
					}
				}
			}
		}
	}
	
	@EventHandler
	public void playerToggleFlight(PlayerToggleFlightEvent event){
		Player player = event.getPlayer();
		if(player.getGameMode() != GameMode.CREATIVE){
			event.setCancelled(true);
			Vector velocity = player.getLocation().getDirection().multiply(1.5).setY(1.7);
			if(player.getLocation().subtract(0, 3, 0).getBlock().getType() == Material.AIR){
				if(main.secondjump.contains(player.getName()) == false){
					player.setVelocity(velocity);
					ParticleUtils.sendToLocation(ParticleEffect.CLOUD, player.getLocation(), 0.7F, 0.7F, 0.7F, 0.1F, 9);
					main.secondjump.add(player.getName());
				}
			}else{
				player.setVelocity(velocity);
				ParticleUtils.sendToLocation(ParticleEffect.CLOUD, player.getLocation(), 0.7F, 0.7F, 0.7F, 0.1F, 9);
				if(main.secondjump.contains(player.getName()) == true){
					main.secondjump.remove(player.getName());
				}
			}
		}
	}
	
	@EventHandler
	public void inventoryClick(InventoryClickEvent event){
		if(event.getWhoClicked() instanceof Player){
			Player player = (Player) event.getWhoClicked();
			SPlayer splayer = new SPlayer(player);
			if(player.hasPermission("saicohub.inventory") == false){
				event.setCancelled(true);
			}
			if(event.getInventory().getTitle().contains("Choose") == true && main.editing.contains(player.getName()) == false){
				if(event.getCurrentItem() != null){
					event.setCancelled(true);
					String server = main.getConfig().getString("servers.selector.inv." + event.getRawSlot() + ".server");
					if(server != null && server.equalsIgnoreCase("none") == false){
						splayer.attemptToConnect(server);
						player.closeInventory();
					}
				}
			}
		}
	}
	
	@EventHandler
	public void playerMove(PlayerMoveEvent event){
		Player player = event.getPlayer();
		for(Entity e : player.getNearbyEntities(0.5, 0.5, 0.5)){
			if((e instanceof Player) == false){
				Location loc = player.getLocation();
				player.teleport(loc.subtract(loc.getDirection().normalize().multiply(1)));
				break;
			}
		}
	}
	
	@EventHandler
	public void entityDamageByEntity(EntityDamageByEntityEvent event){
		if(event.getDamager() instanceof Player){
			Player player = (Player) event.getDamager();
			SPlayer splayer = new SPlayer(player);
			if(main.npcids.containsKey(event.getEntity().getEntityId()) == true){
				String server = main.npcids.get(event.getEntity().getEntityId());
				if(server != null){
					splayer.attemptToConnect(server);
				}
			}
		}
	}
	
	@EventHandler
	public void playerInteractEntity(PlayerInteractEntityEvent event){
		event.setCancelled(true);
		Player player = event.getPlayer();
		SPlayer splayer = new SPlayer(player);
		if(main.npcids.containsKey(event.getRightClicked().getEntityId()) == true){
			String server = main.npcids.get(event.getRightClicked().getEntityId());
			if(server != null){
				splayer.attemptToConnect(server);
			}
		}
	}
	
	@EventHandler
	public void playerDropItem(PlayerDropItemEvent event){
		event.setCancelled(true);
	}
	
	@EventHandler
	public void entityDamage(EntityDamageEvent event){
		event.setCancelled(true);
		event.getEntity().setFireTicks(0);
	}
	
	@EventHandler
	public void foodLevelChange(FoodLevelChangeEvent event){
		event.setCancelled(true);
	}
	
	@EventHandler
	public void entityTarget(EntityTargetEvent event){
		event.setCancelled(true);
		event.setTarget(null);
	}
	
	@EventHandler
	public void creatureSpawn(CreatureSpawnEvent event){
		if(event.getSpawnReason() != SpawnReason.CUSTOM){
			event.setCancelled(true);
		}
	}

}
