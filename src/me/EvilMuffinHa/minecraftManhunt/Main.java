package me.EvilMuffinHa.minecraftManhunt;

import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;


import java.util.ArrayList;

public class Main extends JavaPlugin implements Listener {

	private ArrayList<Player> hunters = new ArrayList<Player>();

	private ArrayList<Player> speedrunners = new ArrayList<Player>();


	private ArrayList<Player> spectators = new ArrayList<Player>();

	private ArrayList<Player> allplayers = new ArrayList<Player>();

	ArrayList<SimpleScoreboard> speedrunBoards = new ArrayList<SimpleScoreboard>();

	ArrayList<SimpleScoreboard> hunterBoards = new ArrayList<SimpleScoreboard>();

	private boolean frozen = false;

	private float[] elements = new float[4*4];






	private int status = 0;
	private Player gameHost;

	@Override
	public void onEnable() {
		getLogger().info("Minecraft Manhunt loading... ");
		saveDefaultConfig();
		Bukkit.getPluginManager().registerEvents(this, this);
		getLogger().info("Minecraft Manhunt loaded! ");



	}



	@Override
	public void onDisable() {
		getLogger().info("Minecraft Manhunt closing... ");
		getLogger().info("Minecraft Manhunt is closed! ");


	}

	@Override
	public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
		switch (command.getName()) {


			case "lobby": {
				Player player = (Player) commandSender;
				if (!player.hasPermission("manhunt.lobbyset")) {
					player.sendMessage(ChatColor.RED + "You cannot set a lobby");
					return false;
				} else {
					Location lobby = player.getLocation();
					getConfig().set("lobby.x", lobby.getX());
					getConfig().set("lobby.y", lobby.getY());
					getConfig().set("lobby.z", lobby.getZ());
					getConfig().set("lobby.world", lobby.getWorld().getName());
					getConfig().set("lobby.pitch", lobby.getPitch());
					getConfig().set("lobby.yaw", lobby.getYaw());
					saveConfig();
					player.sendMessage(ChatColor.GREEN + "Lobby has been set! ");
					return true;
				}

				// break;
			}

			case "spectate": {
				Player player = (Player) commandSender;
				if (!(status == 2)) {
					player.sendMessage(ChatColor.RED + "The game has not started yet! ");
					return false;
				}
				if (!spectators.contains(player)) {
					player.sendMessage(ChatColor.RED + "You are not a spectator! ");
					return false;
				}

				if (strings.length != 1) {
					player.sendMessage(ChatColor.RED + "Please use the right number of arguments. /spectate [player] ");
					return false;
				}

				for (Player h: hunters) {
					if (h.getName().equals(strings[0])) {
						player.teleport(h);
						return true;
					}
				} for (Player r: speedrunners) {
					if (r.getName().equals(strings[0])) {
						player.teleport(r);
						return true;
					}
				}
				player.sendMessage(ChatColor.RED + "Player not found. ");
				return false;
				// break;
			}

			case "opengame": {


				Player player = (Player) commandSender;


				try {
					Location lobby = new Location(Bukkit.getServer().getWorld((String) getConfig().get("lobby.world")),
							(Double) getConfig().get("lobby.x"),
							(Double) getConfig().get("lobby.y"),
							(Double) getConfig().get("lobby.z"),
							(Float) getConfig().get("lobby.pitch"),
							(Float) getConfig().get("lobby.yaw"));

					if (lobby.equals(null)) {
						player.sendMessage(ChatColor.RED + "A lobby has not been set for this server. ");
						return false;
					}

				} catch (Exception e) {
					player.sendMessage(ChatColor.RED + "A lobby has not been set for this server. ");
					return false;
				}

				if (!player.hasPermission("manhunt.opengame")) {
					player.sendMessage(ChatColor.RED + "You cannot open a game. ");
					return false;
				}

				if (status != 0) {
					player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + gameHost.getName() + " has already started a game. ");
					return false;
				}

				Bukkit.getServer().broadcastMessage(ChatColor.GREEN + "" + ChatColor.BOLD + player.getName() + " has opened a game! ");
				status = 1;
				gameHost = player;

				break;
			}


			case "join": {

				Player player = (Player) commandSender;
				if (status == 0) {
					player.sendMessage(ChatColor.RED + "A game has not opened yet! ");
					return false;
				} else if (status == 2) {
					player.sendMessage(ChatColor.RED + "A game is in progress! ");
					return false;
				}
				if (hunters.contains(player) || speedrunners.contains(player)) {
					player.sendMessage(ChatColor.RED + "You are already in the list! ");
					return false;
				}

				if (strings.length != 1) {
					player.sendMessage(ChatColor.RED + "Please use the right number of arguments. ");
					return false;
				}

				if (!strings[0].equals("hunter") && !strings[0].equals("speedrunner") && !strings[0].equals("spectator")) {
					player.sendMessage(ChatColor.RED + "Please use [hunter], [speedrunner], or [spectator] to specify the team. ");
					return false;
				}

				if (strings[0].equals("hunter")) {
					hunters.add(player);
					allplayers.add(player);
					player.sendMessage(ChatColor.GREEN + "Added to HUNTER!");
				} else if (strings[0].equals("speedrunner")) {
					speedrunners.add(player);
					allplayers.add(player);
					player.sendMessage(ChatColor.GREEN + "Added to SPEEDRUNNER!");
				} else {
					spectators.add(player);
					allplayers.add(player);
					player.sendMessage(ChatColor.GREEN + "Added to SPECTATOR!");
				}

				break;
			}


			case "leave": {

				Player player = (Player) commandSender;

				if (status == 2) {
					player.sendMessage(ChatColor.RED + "The game is in progress!");
					return false;
				} else if (status == 0) {
					player.sendMessage(ChatColor.RED + "The game has not opened! ");
					return false;
				}

				if (hunters.contains(player)) {
					hunters.remove(player);
					allplayers.remove(player);
					player.sendMessage(ChatColor.GREEN + "Removed from HUNTER!");
				} else if (speedrunners.contains(player)) {
					speedrunners.remove(player);
					allplayers.remove(player);
					player.sendMessage(ChatColor.GREEN + "Removed from SPEEDRUNNER!");
				} else if (spectators.contains(player)) {
					spectators.remove(player);
					allplayers.remove(player);
					player.sendMessage(ChatColor.GREEN + "Removed from SPECTATOR!");
				} else {
					player.sendMessage(ChatColor.RED + "You are not in the list! ");
					return false;
				}

				break;
			}


			case "view": {
				Player player = (Player) commandSender;

				if (status == 0) {
					player.sendMessage("The game has not opened! ");
					return false;
				}

				StringBuilder hunterList = new StringBuilder("Hunters: ");
				for (Player p: hunters) {
					hunterList.append(p.getName());
					hunterList.append(", ");
				}
				if (hunters.size() != 0) {
					hunterList.delete(hunterList.length() - 2, hunterList.length() - 1);
				}

				StringBuilder speedrunnerList = new StringBuilder("Speedrunners: ");
				for (Player p: speedrunners) {
					speedrunnerList.append(p.getName());
					speedrunnerList.append(", ");
				}
				if (speedrunners.size() != 0) {
					speedrunnerList.delete(speedrunnerList.length() - 2, speedrunnerList.length() - 1);
				}

				StringBuilder spectatorList = new StringBuilder("Spectators: ");
				for (Player p: spectators) {
					spectatorList.append(p.getName());
					spectatorList.append(", ");
				}
				if (spectators.size() != 0) {
					spectatorList.delete(spectatorList.length() - 2, spectatorList.length() - 1);
				}

				Integer hunterNum = hunters.size();
				Integer speedrunnerNum = speedrunners.size();
				Integer totalNum = hunterNum + speedrunnerNum;
				player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Players (" + totalNum.toString() + "): Hunters (" + hunterNum.toString() + "), Speedrunners (" + speedrunnerNum.toString() + ")");
				player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + hunterList.toString());
				player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + speedrunnerList.toString());
				player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + spectatorList.toString());

				break;


			}


			case "start": {
				Player player = (Player) commandSender;

				if (status != 1) {
					player.sendMessage(ChatColor.RED + "The game has already started! ");
					return false;
				}
				if (!player.equals(gameHost)) {
					player.sendMessage(ChatColor.RED + "You are not gamehost!");
					return false;
				}

				if (hunters.size() == 0) {
					player.sendMessage(ChatColor.RED + "There aren't enough hunters to start a game! ");
					return false;
				} else if (speedrunners.size() == 0) {
					player.sendMessage(ChatColor.RED + "There aren't enough speedrunners to start a game! ");
					return false;
				} else {



					status = 2;


					for (Player p: speedrunners) {
						SimpleScoreboard tempboard = new SimpleScoreboard("&e&l MINECRAFT MANHUNT");
						tempboard.add("&bRole: Speedrunner", 1);
						speedrunBoards.add(tempboard);
						tempboard.update();
						tempboard.send(p);

					}
					for (Player p: hunters) {
						SimpleScoreboard tempboard = new SimpleScoreboard("&e&l MINECRAFT MANHUNT");
						tempboard.add("&bRole: Hunter", 1);
						hunterBoards.add(tempboard);
						tempboard.update();
						tempboard.send(p);

					}
					for (Player p: spectators) {
						SimpleScoreboard tempboard = new SimpleScoreboard("&e&l MINECRAFT MANHUNT");
						tempboard.add("&bRole: Spectator", 1);
						tempboard.update();
						tempboard.send(p);

					}

					status = 2;

					Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Starting in 10! ");
					for (Player p: allplayers) {
						p.playSound(player.getLocation(), Sound.NOTE_STICKS, 10, 1);
						p.setHealth(20);
						p.setFoodLevel(20);
						p.setSaturation(6);
						frozen = true;
					}

					Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						public void run() {
							Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "9! ");
							for (Player p: allplayers) {
								p.playSound(player.getLocation(), Sound.NOTE_STICKS, 10, 1);
							}
						}
					}, 20);


					Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						public void run() {
							Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "8! ");
							for (Player p: allplayers) {
								p.playSound(player.getLocation(), Sound.NOTE_STICKS, 10, 1);
							}
						}
					}, 40);


					Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						public void run() {
							Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "7! ");
							for (Player p: allplayers) {
								p.playSound(player.getLocation(), Sound.NOTE_STICKS, 10, 1);
							}
						}
					}, 60);


					Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						public void run() {
							Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "6! ");
							for (Player p: allplayers) {
								p.playSound(player.getLocation(), Sound.NOTE_STICKS, 10, 1);
							}
						}
					}, 80);


					Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						public void run() {
							Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "5! ");
							for (Player p: allplayers) {
								p.playSound(player.getLocation(), Sound.NOTE_STICKS, 10, 1);
							}
						}
					}, 100);


					Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						public void run() {
							Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "4! ");
							for (Player p: allplayers) {
								p.playSound(player.getLocation(), Sound.NOTE_STICKS, 10, 1);
							}
						}
					}, 120);


					Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						public void run() {
							Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "3! ");
							for (Player p: allplayers) {
								p.playSound(player.getLocation(), Sound.NOTE_STICKS, 10, 1);
							}
						}
					}, 140);


					Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						public void run() {
							Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "2! ");
							for (Player p: allplayers) {
								p.playSound(player.getLocation(), Sound.NOTE_STICKS, 10, 1);
							}
						}
					}, 160);


					Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						public void run() {
							Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "1! ");
							for (Player p: allplayers) {
								p.playSound(player.getLocation(), Sound.NOTE_STICKS, 10, 1);
							}
						}
					}, 180);

					Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						public void run() {
							Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "GO! ");
							for (Player p: allplayers) {
								p.playSound(player.getLocation(), Sound.NOTE_STICKS, 10, 1);
								p.setHealth(20);
								p.setFoodLevel(20);
								p.setSaturation(6);
								frozen = false;

							}


							for (Player spec: spectators) {
								spec.setGameMode(GameMode.SPECTATOR);
								spec.sendMessage("Teleport to players using /spectate [player]!");
							}


						}
					}, 200);

					/*
					BukkitTask task = new BukkitRunnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub

							Bukkit.getServer().broadcastMessage("test");

						}
					}.runTaskTimer(this, 10, 10);

					 */
					Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
						@Override
						public void run() {
							if (status == 2) {
								for (int i = 0; i < hunters.size(); i++) {
									hunterBoards.get(i).reset();
									hunterBoards.get(i).add("&bRole: Hunter", 1);
									hunterBoards.get(i).update();
									for (int j = 0; j < speedrunners.size(); j++) {
										int tabnum = j + 2;
										String name;
										if (speedrunners.get(j).getLocation().getWorld().getEnvironment() != hunters.get(i).getLocation().getWorld().getEnvironment()) {
											if (speedrunners.get(j).getLocation().getWorld().getEnvironment().name() == "NORMAL") {
												name = "OVERWORLD";
											} else {
												name = speedrunners.get(i).getLocation().getWorld().getEnvironment().name();
											}
											name = name.replace('_', ' ');

											hunterBoards.get(i).add(speedrunners.get(j).getName() + ": &4" + name, tabnum);
											hunterBoards.get(i).update();
											hunterBoards.get(i).send(hunters.get(i));
										} else {
											hunterBoards.get(i).add(speedrunners.get(j).getName() + ": " + arrowDirec(hunters.get(i), speedrunners.get(j)), tabnum);
											hunterBoards.get(i).update();
											hunterBoards.get(i).send(hunters.get(i));

										}

									}
								}


							}
						}



					}, 0L, 1L);




				}

				break;

			}

			case "shout": {

				Player player = (Player) commandSender;

				if (status != 2) {
					return false;
				}

				StringBuilder text = new StringBuilder();
				for (String i: strings) {
					text.append(i);
					text.append(" ");
				}

				Bukkit.broadcastMessage(ChatColor.YELLOW + "[SHOUT]  " + ChatColor.WHITE + player.getName() + ": " + text.toString());

			}


		}
		return true;
	}



	public static String arrowDirec(Player hunter, Player speedrunner) {

		double huntersDirection = normalAbsoluteAngleDegrees(hunter.getLocation().getYaw()) - 180;
		if (huntersDirection > 0) {
			huntersDirection = 180 - huntersDirection;
		} else if (huntersDirection <= 0) {
			huntersDirection = -180 - huntersDirection;
		}
		Location speedrunnersLocation = speedrunner.getLocation();
		Location huntersLocation = hunter.getLocation();
		double b =  -(huntersLocation.getX() - speedrunnersLocation.getX());
		double a = -(huntersLocation.getZ() - speedrunnersLocation.getZ());

		double angle = Math.atan2(b, a);


		angle = Math.toDegrees(angle);

		double trueDirec = (huntersDirection - angle);
		if (trueDirec < 0) {
			trueDirec += 360;
		}

		String pointer;

		if (trueDirec < 45f/2f || trueDirec > 360-45f/2f) {
			pointer = Arrows.UP.getArrow();
		} else if (trueDirec < 135f/2f) {
			pointer = Arrows.UPRIGHT.getArrow();
		} else if (trueDirec < 225f/2f) {
			pointer = Arrows.RIGHT.getArrow();
		} else if (trueDirec < 315f/2f) {
			pointer = Arrows.DOWNRIGHT.getArrow();
		} else if (trueDirec < 405f/2f) {
			pointer = Arrows.DOWN.getArrow();
		} else if (trueDirec < 495f/2f) {
			pointer = Arrows.DOWNLEFT.getArrow();
		} else if (trueDirec < 585f/2f) {
			pointer = Arrows.LEFT.getArrow();
		} else {
			pointer = Arrows.UPLEFT.getArrow();
		}
		return pointer;
	}

	public static double normalAbsoluteAngleDegrees(double angle) {
		return (angle %= 360) >= 0 ? angle : (angle + 360);
	}


	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		if (hunters.contains(e.getPlayer()) && frozen) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent e) {
		Entity entity = e.getEntity();
		if (entity.getType().name().equals(EntityType.ENDER_DRAGON.name()) && status == 2) {
			// speedrunners win
			Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Speedrunners win! ");
			Location lobby = new Location(Bukkit.getServer().getWorld((String) getConfig().get("lobby.world")),
					(Double) getConfig().get("lobby.x"),
					(Double) getConfig().get("lobby.y"),
					(Double) getConfig().get("lobby.z"),
					(Float) getConfig().get("lobby.pitch"),
					(Float) getConfig().get("lobby.yaw"));
			for (Player s: spectators) {
				s.setGameMode(GameMode.SURVIVAL);
			}

			SimpleScoreboard tempboard = new SimpleScoreboard("&e&l MINECRAFT MANHUNT");

			hunters.clear();
			speedrunners.clear();
			spectators.clear();
			for (Player a: allplayers) {
				tempboard.send(a);
				a.getInventory().clear();


				a.getInventory().setHelmet(null);
				a.getInventory().setChestplate(null);
				a.getInventory().setLeggings(null);
				a.getInventory().setBoots(null);
				a.updateInventory();
				a.setHealth(20);
				a.setFoodLevel(20);
				a.setSaturation(6);
				a.teleport(lobby);
				status = 0;
			}

		}

	}


	@EventHandler
	public void onDamage(EntityDamageByEntityEvent e) {
		try {
			Player p = (Player) e.getEntity();
			if (p.getHealth() - e.getDamage() < 1 && speedrunners.contains(p) && speedrunners.size() == 1 && status == 2) {
				e.setCancelled(true);
				// hunters win
				Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Hunters win! ");
				Location lobby = new Location(Bukkit.getServer().getWorld((String) getConfig().get("lobby.world")),
						(Double) getConfig().get("lobby.x"),
						(Double) getConfig().get("lobby.y"),
						(Double) getConfig().get("lobby.z"),
						(Float) getConfig().get("lobby.pitch"),
						(Float) getConfig().get("lobby.yaw"));
				for (Player s: spectators) {
					s.setGameMode(GameMode.SURVIVAL);
				}
				SimpleScoreboard tempboard = new SimpleScoreboard("&e&l MINECRAFT MANHUNT");

				hunters.clear();
				speedrunners.clear();
				spectators.clear();
				for (Player a: allplayers) {
					tempboard.send(a);
					a.getInventory().clear();


					a.getInventory().setHelmet(null);
					a.getInventory().setChestplate(null);
					a.getInventory().setLeggings(null);
					a.getInventory().setBoots(null);
					a.updateInventory();
					a.setHealth(20);
					a.setFoodLevel(20);
					a.setSaturation(6);
					a.teleport(lobby);
					status = 0;
				}
			} else if (p.getHealth() - e.getDamage() < 1 && hunters.contains(p) && status == 2) {
				e.setCancelled(true);
				p.setGameMode(GameMode.SPECTATOR);
				for (ItemStack itemStack : p.getInventory().getContents()) {
					if (itemStack != null) {
						p.getWorld().dropItemNaturally(p.getLocation(), itemStack);
						p.getInventory().removeItem(itemStack);

					}
				}

				for (ItemStack itemStack : p.getInventory().getArmorContents()) {
					if (!itemStack.getType().equals(Material.AIR)) {
						p.getWorld().dropItemNaturally(p.getLocation(), itemStack);
						p.getInventory().removeItem(itemStack);

					}
				}

				p.getInventory().setHelmet(null);
				p.getInventory().setChestplate(null);
				p.getInventory().setLeggings(null);
				p.getInventory().setBoots(null);



				p.getInventory().clear();



				p.setHealth(20);
				p.setFoodLevel(20);
				p.setSaturation(6);
				p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Respawning in 3! ");


				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
					public void run() {
						p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "2! ");
					}
				}, 20);
				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
					public void run() {
						p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "1! ");
					}
				}, 40);
				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
					public void run() {
						p.teleport(p.getWorld().getSpawnLocation());
						p.setGameMode(GameMode.SURVIVAL);
					}
				}, 60);



			} else if (p.getHealth() - e.getDamage() < 1 && speedrunners.contains(p) && status == 2) {

				e.setCancelled(true);
				p.setGameMode(GameMode.SPECTATOR);
				for (ItemStack itemStack : p.getInventory().getContents()) {
					if (itemStack != null) {
						p.getWorld().dropItemNaturally(p.getLocation(), itemStack);
						p.getInventory().removeItem(itemStack);

					}
				}

				for (ItemStack itemStack : p.getInventory().getArmorContents()) {
					if (!itemStack.getType().equals(Material.AIR)) {
						p.getWorld().dropItemNaturally(p.getLocation(), itemStack);
						p.getInventory().removeItem(itemStack);

					}
				}

				p.getInventory().setHelmet(null);
				p.getInventory().setChestplate(null);
				p.getInventory().setLeggings(null);
				p.getInventory().setBoots(null);



				p.getInventory().clear();

				for (int i = 0; i < speedrunners.size(); i++) {
					if (speedrunners.get(i).equals(p)) {
						speedrunBoards.remove(i);

					}
					SimpleScoreboard tempboard = new SimpleScoreboard("&e&l MINECRAFT MANHUNT");
					tempboard.add("&bRole: Spectator", 1);
					tempboard.update();
					tempboard.send(p);

				}

				speedrunners.remove(p);
				spectators.add(p);

				try
				{
					for (ItemStack itemStack : p.getInventory().getContents()) {
						p.getWorld().dropItemNaturally(p.getLocation(), itemStack);
						p.getInventory().removeItem(itemStack);
					}
					for (ItemStack armor : p.getInventory().getArmorContents()) {
						p.getWorld().dropItemNaturally(p.getLocation(), armor);
						p.getInventory().removeItem(armor);
					}
				}
				catch (IllegalArgumentException ignored)
				{

				}

				p.getInventory().clear();

				p.setGameMode(GameMode.SPECTATOR);
				p.updateInventory();
				p.setHealth(20);
				p.setFoodLevel(20);
				p.setSaturation(6);
				p.sendMessage("Teleport to players using /spectate [player]!");



			}

		} catch (Exception ignored) {
			return;
		}


	}

	@EventHandler
	public void onEnvDamage(EntityDamageEvent e) {

		try {
			Player p = (Player) e.getEntity();
			if (p.getHealth() - e.getDamage() < 1 && speedrunners.contains(p) && speedrunners.size() == 1 && status == 2) {
				e.setCancelled(true);
				// hunters win
				Bukkit.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Hunters win! ");
				Location lobby = new Location(Bukkit.getServer().getWorld((String) getConfig().get("lobby.world")),
						(Double) getConfig().get("lobby.x"),
						(Double) getConfig().get("lobby.y"),
						(Double) getConfig().get("lobby.z"),
						(Float) getConfig().get("lobby.pitch"),
						(Float) getConfig().get("lobby.yaw"));
				for (Player s: spectators) {
					s.setGameMode(GameMode.SURVIVAL);
				}
				SimpleScoreboard tempboard = new SimpleScoreboard("&e&l MINECRAFT MANHUNT");

				hunters.clear();
				speedrunners.clear();
				spectators.clear();
				for (Player a: allplayers) {
					tempboard.send(a);
					a.getInventory().clear();


					a.getInventory().setHelmet(null);
					a.getInventory().setChestplate(null);
					a.getInventory().setLeggings(null);
					a.getInventory().setBoots(null);
					a.updateInventory();
					a.setHealth(20);
					a.setFoodLevel(20);
					a.setSaturation(6);
					a.teleport(lobby);
					status = 0;
				}
			} else if (p.getHealth() - e.getDamage() < 1 && hunters.contains(p) && status == 2) {
				e.setCancelled(true);
				p.setGameMode(GameMode.SPECTATOR);
				for (ItemStack itemStack : p.getInventory().getContents()) {
					if (itemStack != null) {
						p.getWorld().dropItemNaturally(p.getLocation(), itemStack);
						p.getInventory().removeItem(itemStack);

					}
				}

				for (ItemStack itemStack : p.getInventory().getArmorContents()) {
					if (!itemStack.getType().equals(Material.AIR)) {
						p.getWorld().dropItemNaturally(p.getLocation(), itemStack);
						p.getInventory().removeItem(itemStack);

					}
				}

				p.getInventory().setHelmet(null);
				p.getInventory().setChestplate(null);
				p.getInventory().setLeggings(null);
				p.getInventory().setBoots(null);



				p.getInventory().clear();



				p.setHealth(20);
				p.setFoodLevel(20);
				p.setSaturation(6);
				p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Respawning in 3! ");


				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
					public void run() {
						p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "2! ");
					}
				}, 20);
				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
					public void run() {
						p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "1! ");
					}
				}, 40);
				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
					public void run() {
						p.teleport(p.getWorld().getSpawnLocation());
						p.setGameMode(GameMode.SURVIVAL);
					}
				}, 60);



			} else if (p.getHealth() - e.getDamage() < 1 && speedrunners.contains(p) && status == 2) {

				e.setCancelled(true);
				p.setGameMode(GameMode.SPECTATOR);
				for (ItemStack itemStack : p.getInventory().getContents()) {
					if (itemStack != null) {
						p.getWorld().dropItemNaturally(p.getLocation(), itemStack);
						p.getInventory().removeItem(itemStack);

					}
				}

				for (ItemStack itemStack : p.getInventory().getArmorContents()) {
					if (!itemStack.getType().equals(Material.AIR)) {
						p.getWorld().dropItemNaturally(p.getLocation(), itemStack);
						p.getInventory().removeItem(itemStack);

					}
				}

				p.getInventory().setHelmet(null);
				p.getInventory().setChestplate(null);
				p.getInventory().setLeggings(null);
				p.getInventory().setBoots(null);



				p.getInventory().clear();

				for (int i = 0; i < speedrunners.size(); i++) {
					if (speedrunners.get(i).equals(p)) {
						speedrunBoards.remove(i);

					}
					SimpleScoreboard tempboard = new SimpleScoreboard("&e&l MINECRAFT MANHUNT");
					tempboard.add("&bRole: Spectator", 1);
					tempboard.update();
					tempboard.send(p);

				}

				speedrunners.remove(p);
				spectators.add(p);

				try
				{
					for (ItemStack itemStack : p.getInventory().getContents()) {
						p.getWorld().dropItemNaturally(p.getLocation(), itemStack);
						p.getInventory().removeItem(itemStack);
					}
					for (ItemStack armor : p.getInventory().getArmorContents()) {
						p.getWorld().dropItemNaturally(p.getLocation(), armor);
						p.getInventory().removeItem(armor);
					}
				}
				catch (IllegalArgumentException ignored)
				{

				}

				p.getInventory().clear();

				p.setGameMode(GameMode.SPECTATOR);
				p.updateInventory();
				p.setHealth(20);
				p.setFoodLevel(20);
				p.setSaturation(6);
				p.sendMessage("Teleport to players using /spectate [player]!");



			}

		} catch (Exception ignored) {
			return;
		}

	}


	@EventHandler
	public void onJoin(PlayerJoinEvent e) {

		SimpleScoreboard tempboard = new SimpleScoreboard("&e&l MINECRAFT MANHUNT");
		tempboard.update();
		tempboard.send(e.getPlayer());
	}


	@EventHandler
	public void sleep(PlayerBedEnterEvent e) {
		e.getPlayer().sendMessage(ChatColor.RED + "You cannot sleep in a bed! ");
		e.setCancelled(true);
	}

	@EventHandler
	public void chat(AsyncPlayerChatEvent e) {
		String message = e.getMessage();
		Player p = e.getPlayer();
		if (status == 2) {
			if (speedrunners.contains(p)) {
				for (Player sendTo: speedrunners) {
					sendTo.sendMessage(p.getName() + ": " + message);
					e.setCancelled(true);
				}
			} else if (hunters.contains(p)) {
				for (Player sendTo: hunters) {
					sendTo.sendMessage(p.getName() + ": " + message);
					e.setCancelled(true);
				}
			} else if (spectators.contains(p)) {
				for (Player sendTo: spectators) {
					sendTo.sendMessage(p.getName() + ": " + message);
					e.setCancelled(true);
				}
			}
		}
	}

}
