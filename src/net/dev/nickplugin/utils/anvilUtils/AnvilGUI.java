package net.dev.nickplugin.utils.anvilUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.dev.nickplugin.main.Main;
import net.dev.nickplugin.utils.ReflectUtils;

public class AnvilGUI {
	
	private Player player;
	public AnvilClickEventHandler handler;
	private static Class<?> blockPosition;
	private static Class<?> containerAnvil;
	private static Class<?> entityHuman;
	private HashMap<AnvilSlot, ItemStack> items = new HashMap<AnvilSlot, ItemStack>();
	private Inventory inv;
	private Listener listener;

	private void loadClasses() {
		blockPosition = ReflectUtils.getNMSClass("BlockPosition");
		containerAnvil = ReflectUtils.getNMSClass("ContainerAnvil");
		entityHuman = ReflectUtils.getNMSClass("EntityHuman");
	}

	public AnvilGUI(final Player player, final AnvilClickEventHandler handler) {
		loadClasses();
		this.player = player;
		this.handler = handler;

		this.listener = new Listener() {
			@EventHandler
			public void onInventoryClick(InventoryClickEvent event) {
				if (event.getWhoClicked() instanceof Player) {

					if (event.getInventory().equals(inv)) {
						event.setCancelled(true);

						ItemStack item = event.getCurrentItem();
						int slot = event.getRawSlot();
						String name = "";

						if (item != null) {
							if (item.hasItemMeta()) {
								ItemMeta meta = item.getItemMeta();

								if (meta.hasDisplayName()) {
									name = meta.getDisplayName();
								}
							}
						}

						AnvilClickEvent clickEvent = new AnvilClickEvent(AnvilSlot.bySlot(slot), name);

						handler.onAnvilClick(clickEvent);

						if (clickEvent.getWillClose()) {
							event.getWhoClicked().closeInventory();
						}

						if (clickEvent.getWillDestroy()) {
							destroy();
						}
					}
				}
			}

			@EventHandler
			public void onInventoryClose(InventoryCloseEvent event) {
				if (event.getPlayer() instanceof Player) {
					Inventory inv = event.getInventory();
					player.setLevel(player.getLevel() - 1);
					if (inv.equals(AnvilGUI.this.inv)) {
						inv.clear();
						destroy();
					}
				}
			}

			@EventHandler
			public void onPlayerQuit(PlayerQuitEvent event) {
				if (event.getPlayer().equals(getPlayer())) {
					player.setLevel(player.getLevel() - 1);
					destroy();
				}
			}
		};

		Bukkit.getPluginManager().registerEvents(listener, Main.getPlugin(Main.class));
	}

	public Player getPlayer() {
		return player;
	}

	public void setSlot(AnvilSlot slot, ItemStack item) {
		items.put(slot, item);
	}

	public void open() throws IllegalAccessException, InvocationTargetException, InstantiationException {
		player.setLevel(player.getLevel() + 1);

		try {
			Object p = player.getClass().getMethod("getHandle").invoke(player);
			Object container = containerAnvil.getConstructor(ReflectUtils.getNMSClass("PlayerInventory"), ReflectUtils.getNMSClass("World"), blockPosition, entityHuman).newInstance(getPlayerField(player, "inventory"), getPlayerField(player, "world"), blockPosition.getConstructor(int.class, int.class, int.class).newInstance(0, 0, 0), p);
			ReflectUtils.getField(ReflectUtils.getNMSClass("Container"), "checkReachable").set(container, false);

			Object bukkitView = invokeMethod("getBukkitView", container);
			inv = (Inventory) invokeMethod("getTopInventory", bukkitView);

			for (AnvilSlot slot : items.keySet()) {
				inv.setItem(slot.getSlot(), items.get(slot));
			}

			int c = (int) invokeMethod("nextContainerCounter", p);

			Constructor<?> chatMessageConstructor = ReflectUtils.getNMSClass("ChatMessage").getConstructor(String.class, Object[].class);
			Object playerConnection = getPlayerField(player, "playerConnection");
			Object packet = ReflectUtils.getNMSClass("PacketPlayOutOpenWindow").getConstructor(int.class, String.class,
					ReflectUtils.getNMSClass("IChatBaseComponent"), int.class).newInstance(c, "minecraft:anvil",
							chatMessageConstructor.newInstance("Repairing", new Object[] {}), 0);

			Method sendPacket = getMethod("sendPacket", playerConnection.getClass(), ReflectUtils.getNMSClass("Packet"));
			sendPacket.invoke(playerConnection, packet);

			Field activeContainerField = ReflectUtils.getField(entityHuman, "activeContainer");
			
			if (activeContainerField != null) {
				activeContainerField.set(p, container);

				ReflectUtils.getField(ReflectUtils.getNMSClass("Container"), "windowId")
						.set(activeContainerField.get(p), c);

				activeContainerField.get(p).getClass().getMethod("addSlotListener", ReflectUtils.getNMSClass("ICrafting")).invoke(activeContainerField.get(p), p);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Method getMethod(String name, Class<?> clazz, Class<?>... args) {
		try {
			Method method = clazz.getMethod(name, args);
			
			return method;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private Object invokeMethod(String name, Object clazz) {
		try {
			return clazz.getClass().getMethod(name).invoke(clazz);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	private Object getPlayerField(Player p, String name) {
		try {
			Object getHandle = p.getClass().getMethod("getHandle").invoke(p);
			
			return getHandle.getClass().getField(name).get(getHandle);
		} catch (Exception e) {
			e.printStackTrace();
			
			return null;
		}
	}

	public void destroy() {
		player = null;
		handler = null;
		items = null;

		HandlerList.unregisterAll(listener);

		listener = null;
	}

	public enum AnvilSlot {
		INPUT_LEFT(0), INPUT_RIGHT(1), OUTPUT(2);

		private int slot;

		private AnvilSlot(int slot) {
			this.slot = slot;
		}

		public static AnvilSlot bySlot(int slot) {
			for (AnvilSlot anvilSlot : values()) {
				if (anvilSlot.getSlot() == slot) {
					return anvilSlot;
				}
			}

			return null;
		}

		public int getSlot() {
			return slot;
		}
	}

	public interface AnvilClickEventHandler {
		void onAnvilClick(AnvilClickEvent event);
	}

	public class AnvilClickEvent {
		private AnvilSlot slot;

		private String name;

		private boolean close = true;
		private boolean destroy = true;

		public AnvilClickEvent(AnvilSlot slot, String name) {
			this.slot = slot;
			this.name = name;
		}

		public AnvilSlot getSlot() {
			return slot;
		}

		public String getName() {
			return name;
		}

		public boolean getWillClose() {
			return close;
		}

		public void setWillClose(boolean close) {
			this.close = close;
		}

		public boolean getWillDestroy() {
			return destroy;
		}

		public void setWillDestroy(boolean destroy) {
			this.destroy = destroy;
		}
	}
}