package com.ktross.horsemount;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.milkbowl.vault.economy.Economy;

import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class HorseMount extends JavaPlugin implements Listener {

	public static Economy econ;
	public boolean economyEnabled;
	public int horseCost = 0;

	public Map<String, Horse.Variant> mountVariants;
	public Map<String, Horse.Style> mountStyles;
	public Map<String, Horse.Color> mountColors;
	public Map<String, Material> mountArmor;
	public boolean DisableSpawning;
	public boolean DisableItemDrops;

	public void onEnable() {

		this.saveDefaultConfig();

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this, this);

		this.DisableSpawning = ((Boolean) this.getConfig().get(
				"disable-spawning") == true) ? true : false;
		this.DisableItemDrops = ((Boolean) this.getConfig().get(
				"disable-item-drops") == true) ? true : false;
		this.horseCost = ((Integer) this.getConfig().get("horseCost") == null) ? 0
				: (Integer) this.getConfig().get("horseCost");

		// Economy
		if (!setupEconomy()) {
			if (horseCost > 0) {
				getLogger().info("Install Vault if you want economy support.");
			}
		}

		this.mountVariants = new HashMap<String, Horse.Variant>();
		this.mountVariants.put("horse", Horse.Variant.HORSE);
		this.mountVariants.put("mule", Horse.Variant.MULE);
		this.mountVariants.put("donkey", Horse.Variant.DONKEY);
		this.mountVariants.put("skeleton", Horse.Variant.SKELETON_HORSE);
		this.mountVariants.put("zombie", Horse.Variant.UNDEAD_HORSE);

		this.mountStyles = new HashMap<String, Horse.Style>();
		this.mountStyles.put("default", Horse.Style.NONE);
		this.mountStyles.put("white", Horse.Style.WHITE);
		this.mountStyles.put("whitefield", Horse.Style.WHITEFIELD);
		this.mountStyles.put("whitedots", Horse.Style.WHITE_DOTS);
		this.mountStyles.put("blackdots", Horse.Style.BLACK_DOTS);

		this.mountColors = new HashMap<String, Horse.Color>();
		this.mountColors.put("white", Horse.Color.WHITE);
		this.mountColors.put("creamy", Horse.Color.CREAMY);
		this.mountColors.put("chestnut", Horse.Color.CHESTNUT);
		this.mountColors.put("brown", Horse.Color.BROWN);
		this.mountColors.put("black", Horse.Color.BLACK);
		this.mountColors.put("gray", Horse.Color.GRAY);
		this.mountColors.put("darkbrown", Horse.Color.DARK_BROWN);

		this.mountArmor = new HashMap<String, Material>();
		this.mountArmor.put("iron", Material.IRON_BARDING);
		this.mountArmor.put("gold", Material.GOLD_BARDING);
		this.mountArmor.put("diamond", Material.DIAMOND_BARDING);

		// Commands
		getCommand("hm").setExecutor(new HorseMountCommandExecutor(this));
		getCommand("horsemount").setExecutor(
				new HorseMountCommandExecutor(this));
		getCommand("mount").setExecutor(new HorseMountCommandExecutor(this));
		getCommand("dismount").setExecutor(new HorseMountCommandExecutor(this));
		getCommand("setmount").setExecutor(new HorseMountCommandExecutor(this));
		getCommand("setarmor").setExecutor(new HorseMountCommandExecutor(this));
		getCommand("showmount")
				.setExecutor(new HorseMountCommandExecutor(this));
		getCommand("spawnmount").setExecutor(
				new HorseMountCommandExecutor(this));

		// Plugin Metrics
		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
			getLogger().info("Failed to submit stats to MCStats.org");
		}

	}

	public void onDisable() {
	}

	public void msgPlayer(CommandSender sender, String message) {
		msgPlayer((Player) sender, message);
	}

	public void msgPlayer(Player player, String message) {
		player.sendMessage(ChatColor.GOLD + "[" + ChatColor.YELLOW
				+ "HorseMount" + ChatColor.GOLD + "]" + ChatColor.GRAY + " "
				+ message);
	}

	public static String capitalize(final String str) {
		return capitalize(str, 1);
	}

	public static String capitalize(final String str, int in) {
		if (str.length() <= 0) {
			return str;
		}
		final char[] buffer = str.toCharArray();
		boolean capNext = true;
		for (int i = 0; i < buffer.length; i++) {
			final char ch = buffer[i];
			if (Character.isWhitespace(ch)) {
				capNext = true;
			} else if (capNext) {
				buffer[i] = (in != -1) ? Character.toTitleCase(ch) : Character
						.toLowerCase(ch);
				capNext = false;
			}
		}
		return new String(buffer);
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer()
				.getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		this.economyEnabled = (econ != null);
		return econ != null;
	}

	public boolean mount(Player player) {

		String HorseVariant;
		String HorseStyle;
		String HorseColor;
		String HorseArmor;
		World world = player.getWorld();
		Location TargetLocation = player.getLocation();

		if (this.getConfig().get("players." + player.getName()) != null) {
			HorseVariant = (String) this.getConfig().get(
					"players." + player.getName() + ".variant");
			HorseStyle = (String) this.getConfig().get(
					"players." + player.getName() + ".style");
			HorseColor = (String) this.getConfig().get(
					"players." + player.getName() + ".color");
			HorseArmor = (String) this.getConfig().get(
					"players." + player.getName() + ".armor");
		} else {
			HorseVariant = (String) this.getConfig().get(
					"players.default.variant");
			HorseStyle = (String) this.getConfig().get("players.default.style");
			HorseColor = (String) this.getConfig().get("players.default.color");
			HorseArmor = (String) this.getConfig().get("players.default.armor");
		}

		if (this.mountVariants.get(HorseVariant) == null
				|| this.mountStyles.get(HorseStyle) == null
				|| this.mountColors.get(HorseColor) == null) {
			if (!HorseStyle.equalsIgnoreCase("none")
					&& !HorseColor.equalsIgnoreCase("none")) {
				this.msgPlayer(player,
						"Mount does not exist. Please check your config values.");
				return false;
			}
		}
		if (HorseStyle.equalsIgnoreCase("none")
				|| HorseColor.equalsIgnoreCase("none")) {
			if (!player.hasPermission("horsemount.variant." + HorseVariant)) {
				this.msgPlayer(player,
						"You do not have permission to use this mount.");
				return false;
			}
		} else {
			if (!player.hasPermission("horsemount.variant." + HorseVariant)
					|| !player.hasPermission("horsemount.style." + HorseStyle)
					|| !player.hasPermission("horsemount.color." + HorseColor)) {
				this.msgPlayer(player,
						"You do not have permission to use this mount.");
				return false;
			}
		}

		Horse horse = (Horse) world.spawnEntity(TargetLocation,
				EntityType.HORSE);
		HorseInventory inv = horse.getInventory();

		horse.setVariant(this.mountVariants.get(HorseVariant));

		if (HorseVariant.equalsIgnoreCase("horse")) {
			horse.setStyle(this.mountStyles.get(HorseStyle));
		}
		if (HorseVariant.equalsIgnoreCase("horse")) {
			horse.setColor(this.mountColors.get(HorseColor));
		}

		inv.setSaddle(new ItemStack(Material.SADDLE, 1));

		if (HorseVariant.equalsIgnoreCase("horse")
				&& !HorseArmor.equalsIgnoreCase("none")
				&& player.hasPermission("horsemount.armor." + HorseArmor)) {
			inv.setArmor(new ItemStack(this.mountArmor.get(HorseArmor), 1));
		}

		horse.setOwner(player);
		horse.setPassenger(player);
		return true;
	}

	@EventHandler
	public void onVehicleEnter(VehicleEnterEvent event) {
		Player player = (Player) event.getEntered();
		if (player.getVehicle() instanceof Horse) {
			player.getVehicle().remove();
			msgPlayer(player, "Automatically dismounted due to vehicle change.");
		}
	}

	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof Horse
				&& event.getRightClicked().isEmpty()
				&& event.getPlayer().getItemInHand().getType() != Material.LEASH) {
			boolean eventCancelled = true;
			Player p = (Player) event.getPlayer();
			LivingEntity h = (LivingEntity) event.getRightClicked();
			if (p.hasPermission("horsemount.mount")
					&& (!h.getCustomName().equalsIgnoreCase("[HM] Display") || p
							.hasPermission("horsemount.spawnmount"))) {
				eventCancelled = false;
			} else {
				msgPlayer(p, "You do not have permission to mount this horse.");
			}
			event.setCancelled(eventCancelled);
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getInventory() instanceof HorseInventory
				&& event.getSlotType() != InventoryType.SlotType.QUICKBAR
				&& event.getSlot() < 9) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onVehicleExit(VehicleExitEvent event) {
		if (event.getVehicle() instanceof Horse) {
			Horse h = (Horse) event.getVehicle();
			h.remove();
		}
	}

	@EventHandler
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (event.getEntityType() == EntityType.HORSE
				&& event.getSpawnReason() != SpawnReason.CUSTOM
				&& this.DisableSpawning == true) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntityType() == EntityType.HORSE
				&& ((LivingEntity) event.getEntity()).getCustomName() != null
				&& ((LivingEntity) event.getEntity()).getCustomName()
						.equalsIgnoreCase("[HM] Display")) {
			event.setCancelled(true);
		}
		if (event.getEntityType() == EntityType.PLAYER
				&& event.getEntity().getVehicle() != null) {
			Damageable p = (Damageable) event.getEntity();
			if (event.getDamage() >= p.getHealth()) {
				Horse h = (Horse) event.getEntity().getVehicle();
				h.remove();
			}
		}
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		if (event.getEntityType() == EntityType.HORSE
				&& this.DisableItemDrops == true) {
			event.getDrops().clear();
			event.setDroppedExp(0);
		}
	}

	@EventHandler
	public void onItemSpawn(ItemSpawnEvent event) {
		if ((event.getEntity().getItemStack().getType() == Material.SADDLE
				|| event.getEntity().getItemStack().getType() == Material.IRON_BARDING
				|| event.getEntity().getItemStack().getType() == Material.GOLD_BARDING || event
				.getEntity().getItemStack().getType() == Material.DIAMOND_BARDING)
				&& this.DisableItemDrops == true) {
			// Workaround to prevent saddle/armor drops due to bukkit bug
			if (event.getEntity().getVelocity().getY() == 0.20000000298023224) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onSignChange(SignChangeEvent event) {
		if ((event.getLine(0).equalsIgnoreCase("[HM]") || event.getLine(0)
				.equalsIgnoreCase("[HorseMount]"))
				&& event.getPlayer().hasPermission("horsemount.signs.create")) {
			if (this.mountArmor.get(event.getLine(1)) != null) {
				event.setLine(0, ChatColor.AQUA + "[HorseMount]");
				event.setLine(1, capitalize(event.getLine(1)));
			} else {
				if (this.mountVariants.get(event.getLine(1)) != null
						&& !event.getLine(1).equalsIgnoreCase("horse")) {
					event.setLine(0, ChatColor.AQUA + "[HorseMount]");
					event.setLine(1, capitalize(event.getLine(1)));
				} else if (this.mountVariants.get(event.getLine(1)) != null
						&& this.mountStyles.get(event.getLine(2)) != null
						&& this.mountColors.get(event.getLine(3)) != null) {
					event.setLine(0, ChatColor.AQUA + "[HorseMount]");
					event.setLine(1, capitalize(event.getLine(1)));
					event.setLine(2, capitalize(event.getLine(2)));
					event.setLine(3, capitalize(event.getLine(3)));
				} else if (event.getLine(1).equalsIgnoreCase("mount")){
					event.setLine(0, ChatColor.AQUA + "[HorseMount]");
					event.setLine(1, capitalize(event.getLine(1)));
					event.setLine(2, capitalize(event.getLine(2)));
					event.setLine(3, capitalize(event.getLine(3)));
				} else {
					event.setLine(0, "Error:");
					event.setLine(1, "Invalid");
					event.setLine(2, "Parameters");
					event.setLine(3, "");
				}
			}
		}
	}

	@EventHandler
	public void onRightClick(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK
				&& event.getClickedBlock().getState() instanceof Sign) {
			Sign clickedSign = (Sign) event.getClickedBlock().getState();
			if (ChatColor.stripColor(clickedSign.getLine(0)).equalsIgnoreCase(
					"[HorseMount]")
					&& event.getPlayer().hasPermission("horsemount.signs.use")) {
				if (economyEnabled
						&& econ.has(event.getPlayer(), this.horseCost)) {
					if (clickedSign.getLine(1).equalsIgnoreCase("mount")) {
						econ.withdrawPlayer(event.getPlayer(),
								(double) this.horseCost);
						this.mount(event.getPlayer());
					}
				} else {
					msgPlayer(
							event.getPlayer(),
							"Sorry, you do not have enough money to rent this horse, you still need "
									+ econ.format(this.horseCost - econ
											.getBalance(event.getPlayer())));

					boolean mountArmorExists = false;
					for (String key : mountArmor.keySet()) {
						if (clickedSign.getLine(1).equalsIgnoreCase(key)) {
							mountArmorExists = true;
							break;
						}
					}
					if (mountArmorExists) {
						PluginCommand setArmor = getServer().getPluginCommand(
								"setarmor");
						String armorArgs[];
						armorArgs = new String[1];
						armorArgs[0] = capitalize(clickedSign.getLine(1), -1);
						setArmor.execute(event.getPlayer(), "setarmor",
								armorArgs);
					} else {
						String[] lines = clickedSign.getLines();
						int argsCount = 0;
						for (String line : lines) {
							if (!line.equalsIgnoreCase("")
									&& !ChatColor.stripColor(line)
											.equalsIgnoreCase("[HorseMount]")) {
								argsCount++;
							}
						}
						PluginCommand setMount = getServer().getPluginCommand(
								"setmount");
						String[] mountArgs;
						mountArgs = new String[argsCount];
						int count = 0;
						while (count < argsCount) {
							mountArgs[count] = WordUtils
									.uncapitalize(lines[count + 1]);
							count++;
						}
						setMount.execute(event.getPlayer(), "setmount",
								mountArgs);
					}
				}
			}
		}
	}
}
