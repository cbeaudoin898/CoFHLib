package cofh.lib.util.helpers;

import java.io.File;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;

import cofh.api.tileentity.ISecurable;
import cofh.api.tileentity.ISecurable.AccessMode;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PreYggdrasilConverter;

public final class SecurityHelper {

	public static final GameProfile UNKNOWN_GAME_PROFILE = new GameProfile(UUID.fromString("1ef1a6f0-87bc-4e78-0a0b-c6824eb787ea"), "[None]");

	private SecurityHelper() {

	}

	public static boolean isDefaultUUID(UUID uuid) {
		return uuid == null || (uuid.version() == 4 && uuid.variant() == 0);
	}

	public static UUID getID(EntityPlayer player) {

		try{
			if (MinecraftServer.USER_CACHE_FILE.exists() && MinecraftServer.getCurrentTimeMillis() > 0) {
				return player.getGameProfile().getId();
			}
			return getClientId(player);
		}catch (Exception e){
			//TODO: Add in decent error logging here. Hopefully we have a server running if this is being called.
			return null; //Yeah, if there's no Server we shouldn't be checking ID's should we?
		}
	}

	private static UUID cachedId;

	private static UUID getClientId(EntityPlayer player) {

		if (player != Minecraft.getMinecraft().thePlayer) {
			return player.getGameProfile().getId();
		}
		if (cachedId == null) {
			cachedId = Minecraft.getMinecraft().thePlayer.getGameProfile().getId();
		}
		return cachedId;
	}

	/* NBT TAG HELPER */
	public static NBTTagCompound setItemStackTagSecure(NBTTagCompound tag, ISecurable tile) {

		if (tile == null) {
			return null;
		}
		if (tag == null) {
			tag = new NBTTagCompound();
		}
		tag.setBoolean("Secure", true);
		tag.setByte("Access", (byte) tile.getAccess().ordinal());
		tag.setString("OwnerUUID", tile.getOwner().getId().toString());
		tag.setString("Owner", tile.getOwner().getName());
		return tag;
	}

	/**
	 * Adds Security information to ItemStacks.
	 */
	public static void addOwnerInformation(ItemStack stack, List<String> list) {

		if (SecurityHelper.isSecure(stack)) {
			boolean hasUUID = stack.getTagCompound().hasKey("OwnerUUID");
			if (!stack.getTagCompound().hasKey("Owner") && !hasUUID) {
				list.add(StringHelper.localize("info.cofh.owner") + ": " + StringHelper.localize("info.cofh.none"));
			} else {
				if (hasUUID && stack.getTagCompound().hasKey("Owner")) {
					list.add(StringHelper.localize("info.cofh.owner") + ": " + stack.getTagCompound().getString("Owner") + " \u0378");
				} else {
					list.add(StringHelper.localize("info.cofh.owner") + ": " + StringHelper.localize("info.cofh.anotherplayer"));
				}
			}
		}
	}

	public static void addAccessInformation(ItemStack stack, List<String> list) {

		if (SecurityHelper.isSecure(stack)) {
			String accessString = "";
			switch (ISecurable.AccessMode.values()[stack.getTagCompound().getByte("Access")]) {
			case PUBLIC:
				accessString = StringHelper.localize("info.cofh.accessPublic");
				break;
			case GUILD:
				accessString = StringHelper.localize("info.cofh.accessGuild");
				break;
			case RESTRICTED:
				accessString = StringHelper.localize("info.cofh.accessRestricted");
				break;
			case PRIVATE:
				accessString = StringHelper.localize("info.cofh.accessPrivate");
				break;
			}
			list.add(StringHelper.localize("info.cofh.access") + ": " + accessString);
		}
	}

	/* ITEM HELPERS */
	public static boolean isSecure(ItemStack stack) {

		return !stack.hasTagCompound() ? false : stack.getTagCompound().hasKey("Secure");
	}

	public static ItemStack setSecure(ItemStack stack) {

		if (isSecure(stack)) {
			return stack;
		}
		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setBoolean("Secure", true);
		stack.getTagCompound().setByte("Access", (byte) 0);
		return stack;
	}

	public static ItemStack removeSecure(ItemStack stack) {

		if (!isSecure(stack)) {
			return stack;
		}
		stack.getTagCompound().removeTag("Secure");
		stack.getTagCompound().removeTag("Access");
		stack.getTagCompound().removeTag("OwnerUUID");
		stack.getTagCompound().removeTag("Owner");

		if (stack.getTagCompound().hasNoTags()) {
			stack.setTagCompound(null);
		}
		return stack;
	}

	public static boolean setAccess(ItemStack stack, AccessMode access) {

		if (!isSecure(stack)) {
			return false;
		}
		stack.getTagCompound().setByte("Access", (byte) access.ordinal());
		return true;
	}

	public static AccessMode getAccess(ItemStack stack) {

		return !stack.hasTagCompound() ? AccessMode.PUBLIC : AccessMode.values()[stack.getTagCompound().getByte("Access")];
	}

	public static boolean setOwner(ItemStack stack, GameProfile name) {

		if (!isSecure(stack)) {
			return false;
		}
		stack.setTagInfo("OwnerUUID", new NBTTagString(name.getId().toString()));
		stack.setTagInfo("Owner", new NBTTagString(name.getName()));
		return true;
	}

	//TODO:PreYggdrasilConverter is getting phased out. Rework this section using the cache file instead
	//TODO: Create a helper for access to the usercache json file as that's probably useful...
	//maybe even use netCLientHandler - it has some handy methods for this
	
	public static GameProfile getOwner(ItemStack stack) {
		File userCache = MinecraftServer.USER_CACHE_FILE; //Json
		if (stack.getTagCompound() != null) {
			NBTTagCompound nbt = stack.getTagCompound();
			String uuid = nbt.getString("OwnerUUID");
			String name = nbt.getString("Owner");
			if (!Strings.isNullOrEmpty(uuid)) {
				return new GameProfile(UUID.fromString(uuid), name);
			} else if (!Strings.isNullOrEmpty(name)) {
				//return new GameProfile(UUID.fromString(PreYggdrasilConverter.getStringUUIDFromName(name)), name);
				return UNKNOWN_GAME_PROFILE;
			}
		}
		return UNKNOWN_GAME_PROFILE;
	}
	//TODO:Update this method once I build the usercache helper
/*
	public static GameProfile getProfile(UUID uuid, String name) {

		GameProfile owner = MinecraftServer.getPlayerProfileCache().getProfileByUUID(uuid);
		if (owner == null) {
			GameProfile temp = new GameProfile(uuid, name);
			owner = MinecraftServer.getServer().getMinecraftSessionService().fillProfileProperties(temp, true);
			if (owner != temp) {
				MinecraftServer.getServer().getPlayerProfileCache().addEntry(owner);
			}
		}
		return owner;
	}
*/
	public static String getOwnerName(ItemStack stack) {

		NBTTagCompound nbt = stack.getTagCompound();
		boolean hasUUID;
		if (nbt == null || (!(hasUUID = nbt.hasKey("OwnerUUID")) && !nbt.hasKey("Owner"))) {
			return "[None]";
		}
		return hasUUID ? stack.getTagCompound().getString("Owner") : StringHelper.localize("info.cofh.anotherplayer");
	}

}
