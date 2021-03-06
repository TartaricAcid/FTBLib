package com.feed_the_beast.ftblib.lib.data;

import com.feed_the_beast.ftblib.FTBLib;
import com.feed_the_beast.ftblib.events.team.ForgeTeamConfigEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamDataEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamDeletedEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamOwnerChangedEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamPlayerJoinedEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamPlayerLeftEvent;
import com.feed_the_beast.ftblib.lib.EnumTeamColor;
import com.feed_the_beast.ftblib.lib.EnumTeamStatus;
import com.feed_the_beast.ftblib.lib.config.ConfigBoolean;
import com.feed_the_beast.ftblib.lib.config.ConfigEnum;
import com.feed_the_beast.ftblib.lib.config.ConfigGroup;
import com.feed_the_beast.ftblib.lib.config.ConfigString;
import com.feed_the_beast.ftblib.lib.config.IConfigCallback;
import com.feed_the_beast.ftblib.lib.icon.Icon;
import com.feed_the_beast.ftblib.lib.icon.PlayerHeadIcon;
import com.feed_the_beast.ftblib.lib.util.FileUtils;
import com.feed_the_beast.ftblib.lib.util.FinalIDObject;
import com.feed_the_beast.ftblib.lib.util.StringUtils;
import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class ForgeTeam extends FinalIDObject implements IStringSerializable, INBTSerializable<NBTTagCompound>, IHasCache, IConfigCallback
{
	public final Universe universe;
	public final TeamType type;
	public ForgePlayer owner;
	private final NBTDataStorage dataStorage;
	private final ConfigString title;
	private final ConfigString desc;
	private final ConfigEnum<EnumTeamColor> color;
	private final ConfigString icon;
	private final ConfigBoolean freeToJoin;
	private final ConfigEnum<EnumTeamStatus> fakePlayerStatus;
	private final Collection<ForgePlayer> requestingInvite;
	public final Map<ForgePlayer, EnumTeamStatus> players;
	private ConfigGroup cachedConfig;
	private ITextComponent cachedTitle;
	private Icon cachedIcon;
	public boolean needsSaving;

	public ForgeTeam(Universe u, String id, TeamType t)
	{
		super(id, t.isNone ? 0 : (StringUtils.FLAG_ID_DEFAULTS | StringUtils.FLAG_ID_ALLOW_EMPTY));
		universe = u;
		type = t;
		title = new ConfigString("");
		desc = new ConfigString("");
		color = new ConfigEnum<>(EnumTeamColor.NAME_MAP);
		icon = new ConfigString("");
		freeToJoin = new ConfigBoolean(false);
		fakePlayerStatus = new ConfigEnum<>(EnumTeamStatus.NAME_MAP_PERMS);
		requestingInvite = new HashSet<>();
		players = new HashMap<>();
		dataStorage = new NBTDataStorage();
		new ForgeTeamDataEvent(this, dataStorage::add).post();
		clearCache();
		cachedIcon = null;
		needsSaving = false;
	}

	@Override
	public NBTTagCompound serializeNBT()
	{
		NBTTagCompound nbt = new NBTTagCompound();
		if (owner != null)
		{
			nbt.setString("Owner", owner.getName());
		}

		nbt.setString("Title", title.getString());
		nbt.setString("Desc", desc.getString());
		nbt.setString("Color", color.getString());
		nbt.setString("Icon", icon.getString());
		nbt.setBoolean("FreeToJoin", freeToJoin.getBoolean());
		nbt.setString("FakePlayerStatus", fakePlayerStatus.getString());

		NBTTagCompound nbt1 = new NBTTagCompound();

		if (!players.isEmpty())
		{
			for (Map.Entry<ForgePlayer, EnumTeamStatus> entry : players.entrySet())
			{
				nbt1.setString(entry.getKey().getName(), entry.getValue().getName());
			}
		}

		nbt.setTag("Players", nbt1);

		NBTTagList list = new NBTTagList();

		for (ForgePlayer player : requestingInvite)
		{
			list.appendTag(new NBTTagString(player.getName()));
		}

		nbt.setTag("RequestingInvite", list);
		nbt.setTag("Data", dataStorage.serializeNBT());
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt)
	{
		owner = universe.getPlayer(nbt.getString("Owner"));

		if (!isValid())
		{
			return;
		}

		title.setString(nbt.getString("Title"));
		desc.setString(nbt.getString("Desc"));
		color.setValue(nbt.getString("Color"));
		icon.setString(nbt.getString("Icon"));
		freeToJoin.setBoolean(nbt.getBoolean("FreeToJoin"));
		fakePlayerStatus.setValue(nbt.getString("FakePlayerStatus"));

		players.clear();

		if (nbt.hasKey("Players"))
		{
			NBTTagCompound nbt1 = nbt.getCompoundTag("Players");

			for (String s : nbt1.getKeySet())
			{
				ForgePlayer player = universe.getPlayer(s);

				if (player != null)
				{
					EnumTeamStatus status = EnumTeamStatus.NAME_MAP.get(nbt1.getString(s));

					if (status.canBeSet())
					{
						setStatus(player, status);
					}
				}
			}
		}

		NBTTagList list = nbt.getTagList("RequestingInvite", Constants.NBT.TAG_STRING);

		for (int i = 0; i < list.tagCount(); i++)
		{
			ForgePlayer player = universe.getPlayer(list.getStringTagAt(i));

			if (player != null && !isMember(player))
			{
				setRequestingInvite(player, true);
			}
		}

		list = nbt.getTagList("Invited", Constants.NBT.TAG_STRING);

		for (int i = 0; i < list.tagCount(); i++)
		{
			ForgePlayer player = universe.getPlayer(list.getStringTagAt(i));

			if (player != null && !isMember(player))
			{
				setStatus(player, EnumTeamStatus.INVITED);
			}
		}

		dataStorage.deserializeNBT(nbt.getCompoundTag("Data"));
	}

	@Override
	public void clearCache()
	{
		cachedTitle = null;
		cachedIcon = null;
		cachedConfig = null;
		dataStorage.clearCache();
	}

	public void markDirty()
	{
		needsSaving = true;
		universe.checkSaving = true;
	}

	public NBTDataStorage getData()
	{
		return dataStorage;
	}

	public boolean hasOwner()
	{
		return type.isPlayer && owner != null;
	}

	public ITextComponent getTitle()
	{
		if (cachedTitle != null)
		{
			return cachedTitle;
		}

		if (title.isEmpty())
		{
			cachedTitle = hasOwner() ? owner.getDisplayName().appendText("'s Team") : new TextComponentString("Unnamed");
		}
		else
		{
			cachedTitle = new TextComponentString(title.getString());
		}

		cachedTitle = StringUtils.color(cachedTitle, getColor().getTextFormatting());
		return cachedTitle;
	}

	public void setTitle(String s)
	{
		if (!title.getString().equals(s))
		{
			title.setString(s);
			markDirty();
		}
	}

	public String getDesc()
	{
		return desc.getString();
	}

	public void setDesc(String s)
	{
		if (!desc.getString().equals(s))
		{
			desc.setString(s);
			markDirty();
		}
	}

	public EnumTeamColor getColor()
	{
		return color.getValue();
	}

	public void setColor(EnumTeamColor col)
	{
		if (color.getValue() != col)
		{
			color.setValue(col);
			markDirty();
		}
	}

	public Icon getIcon()
	{
		if (cachedIcon == null)
		{
			String iconstring = icon.getString();
			if (iconstring.isEmpty())
			{
				if (hasOwner())
				{
					cachedIcon = new PlayerHeadIcon(owner.getProfile().getId());
				}
				else
				{
					cachedIcon = getColor().getColor();
				}
			}
			else
			{
				cachedIcon = Icon.getIcon(iconstring);
			}
		}

		return cachedIcon;
	}

	public void setIcon(String s)
	{
		if (!icon.getString().equals(s))
		{
			icon.setString(s);
			markDirty();
		}
	}

	public boolean isFreeToJoin()
	{
		return freeToJoin.getBoolean();
	}

	public void setFreeToJoin(boolean b)
	{
		if (freeToJoin.getBoolean() != b)
		{
			freeToJoin.setBoolean(b);
			markDirty();
		}
	}

	public EnumTeamStatus getFakePlayerStatus(ForgePlayer player)
	{
		return fakePlayerStatus.getValue();
	}

	public EnumTeamStatus getHighestStatus(@Nullable ForgePlayer player)
	{
		if (player == null)
		{
			return EnumTeamStatus.NONE;
		}
		else if (player.isFake())
		{
			return fakePlayerStatus.getValue();
		}
		else if (isOwner(player))
		{
			return EnumTeamStatus.OWNER;
		}
		else if (isModerator(player))
		{
			return EnumTeamStatus.MOD;
		}
		else if (isMember(player))
		{
			return EnumTeamStatus.MEMBER;
		}
		else if (isEnemy(player))
		{
			return EnumTeamStatus.ENEMY;
		}
		else if (isAlly(player))
		{
			return EnumTeamStatus.ALLY;
		}
		else if (isInvited(player))
		{
			return EnumTeamStatus.INVITED;
		}

		return EnumTeamStatus.NONE;
	}

	private EnumTeamStatus getSetStatus(@Nullable ForgePlayer player)
	{
		if (player == null || !isValid())
		{
			return EnumTeamStatus.NONE;
		}
		else if (player.isFake())
		{
			return fakePlayerStatus.getValue();
		}
		else if (type == TeamType.SERVER && getName().equals("singleplayer"))
		{
			return EnumTeamStatus.MOD;
		}

		EnumTeamStatus status = players.get(player);
		return status == null ? EnumTeamStatus.NONE : status;
	}

	public boolean hasStatus(@Nullable ForgePlayer player, EnumTeamStatus status)
	{
		if (player == null || !isValid())
		{
			return false;
		}

		if (player.isFake())
		{
			return getFakePlayerStatus(player).isEqualOrGreaterThan(status);
		}

		switch (status)
		{
			case NONE:
				return true;
			case ENEMY:
				return isEnemy(player);
			case ALLY:
				return isAlly(player);
			case INVITED:
				return isInvited(player);
			case MEMBER:
				return isMember(player);
			case MOD:
				return isModerator(player);
			case OWNER:
				return isOwner(player);
			default:
				return false;
		}
	}

	public boolean setStatus(@Nullable ForgePlayer player, EnumTeamStatus status)
	{
		if (player == null || !isValid() || player.isFake())
		{
			return false;
		}
		else if (status == EnumTeamStatus.OWNER)
		{
			if (!isMember(player))
			{
				return false;
			}

			if (!player.equalsPlayer(owner))
			{
				universe.clearCache();
				ForgePlayer oldOwner = owner;
				owner = player;
				players.remove(player);
				new ForgeTeamOwnerChangedEvent(this, oldOwner).post();

				if (oldOwner != null)
				{
					oldOwner.markDirty();
				}

				owner.markDirty();
				markDirty();
				return true;
			}

			return false;
		}
		else if (!status.isNone() && status.canBeSet())
		{
			if (players.put(player, status) != status)
			{
				universe.clearCache();
				player.markDirty();
				markDirty();
				return true;
			}
		}
		else if (players.remove(player) != status)
		{
			universe.clearCache();
			player.markDirty();
			markDirty();
			return true;
		}

		return false;
	}

	public <C extends Collection<ForgePlayer>> C getPlayersWithStatus(C collection, EnumTeamStatus status)
	{
		if (!isValid())
		{
			return collection;
		}

		for (ForgePlayer player : universe.getPlayers())
		{
			if (!player.isFake() && hasStatus(player, status))
			{
				collection.add(player);
			}
		}

		return collection;
	}

	public List<ForgePlayer> getPlayersWithStatus(EnumTeamStatus status)
	{
		return isValid() ? getPlayersWithStatus(new ArrayList<>(), status) : Collections.emptyList();
	}

	public boolean addMember(ForgePlayer player, boolean simulate)
	{
		if (isValid() && ((isOwner(player) || isInvited(player)) && !isMember(player)))
		{
			if (!simulate)
			{
				universe.clearCache();
				player.team = this;
				players.remove(player);
				requestingInvite.remove(player);
				new ForgeTeamPlayerJoinedEvent(player).post();
				player.markDirty();
				markDirty();
			}

			return true;
		}

		return false;
	}

	public boolean removeMember(ForgePlayer player)
	{
		if (!isValid() || !isMember(player))
		{
			return false;
		}
		else if (getMembers().size() == 1)
		{
			universe.clearCache();
			new ForgeTeamPlayerLeftEvent(player).post();

			if (type.isPlayer)
			{
				delete();
			}
			else
			{
				setStatus(player, EnumTeamStatus.NONE);
			}

			player.team = universe.getTeam("");
			player.markDirty();
			markDirty();
		}
		else if (isOwner(player))
		{
			return false;
		}

		universe.clearCache();
		new ForgeTeamPlayerLeftEvent(player).post();
		player.team = universe.getTeam("");
		setStatus(player, EnumTeamStatus.NONE);
		player.markDirty();
		markDirty();
		return true;
	}

	public void delete()
	{
		File folder = new File(universe.getWorldDirectory(), "data/ftb_lib/teams/");
		new ForgeTeamDeletedEvent(this, folder).post();
		universe.teams.remove(getName());
		FileUtils.delete(new File(folder, getName() + ".dat"));
		universe.markDirty();
		universe.clearCache();
	}

	public List<ForgePlayer> getMembers()
	{
		return getPlayersWithStatus(EnumTeamStatus.MEMBER);
	}

	public boolean isMember(@Nullable ForgePlayer player)
	{
		if (player == null)
		{
			return false;
		}
		else if (player.isFake())
		{
			return fakePlayerStatus.getValue().isEqualOrGreaterThan(EnumTeamStatus.MEMBER);
		}

		return isValid() && equalsTeam(player.team);
	}

	public boolean isAlly(@Nullable ForgePlayer player)
	{
		return isValid() && (isMember(player) || getSetStatus(player).isEqualOrGreaterThan(EnumTeamStatus.ALLY));
	}

	public boolean isInvited(@Nullable ForgePlayer player)
	{
		return isValid() && (isMember(player) || ((isFreeToJoin() || getSetStatus(player).isEqualOrGreaterThan(EnumTeamStatus.INVITED)) && !isEnemy(player)));
	}

	public boolean setRequestingInvite(@Nullable ForgePlayer player, boolean value)
	{
		if (player != null && isValid())
		{
			if (value)
			{
				if (requestingInvite.add(player))
				{
					player.markDirty();
					markDirty();
					return true;
				}
			}
			else if (requestingInvite.remove(player))
			{
				player.markDirty();
				markDirty();
				return true;
			}

			return false;
		}

		return false;
	}

	public boolean isRequestingInvite(@Nullable ForgePlayer player)
	{
		return player != null && isValid() && !isMember(player) && requestingInvite.contains(player) && !isEnemy(player);
	}

	public boolean isEnemy(@Nullable ForgePlayer player)
	{
		return getSetStatus(player) == EnumTeamStatus.ENEMY;
	}

	public boolean isModerator(@Nullable ForgePlayer player)
	{
		return isOwner(player) || isMember(player) && getSetStatus(player).isEqualOrGreaterThan(EnumTeamStatus.MOD);
	}

	public boolean isOwner(@Nullable ForgePlayer player)
	{
		return player != null && player.equalsPlayer(owner);
	}

	public ConfigGroup getSettings()
	{
		if (cachedConfig == null)
		{
			cachedConfig = ConfigGroup.newGroup("team_config");
			cachedConfig.setDisplayName(new TextComponentTranslation("gui.settings"));
			ForgeTeamConfigEvent event = new ForgeTeamConfigEvent(this, cachedConfig);
			event.post();

			ConfigGroup main = cachedConfig.getGroup(FTBLib.MOD_ID);
			main.setDisplayName(new TextComponentString(FTBLib.MOD_NAME));
			main.add("free_to_join", freeToJoin, new ConfigBoolean(false));

			ConfigGroup display = main.getGroup("display");
			display.add("color", color, new ConfigEnum<>(EnumTeamColor.NAME_MAP));
			display.add("fake_player_status", fakePlayerStatus, new ConfigEnum<>(EnumTeamStatus.NAME_MAP_PERMS));
			display.add("title", title, new ConfigString(""));
			display.add("desc", desc, new ConfigString(""));
		}

		return cachedConfig;
	}

	public boolean isValid()
	{
		if (type.isNone)
		{
			return false;
		}

		return type.isServer || hasOwner();
	}

	public boolean equalsTeam(@Nullable ForgeTeam team)
	{
		return team == this || (team != null && getName().equals(team.getName()));
	}

	public boolean anyPlayerHasPermission(String permission, EnumTeamStatus status)
	{
		for (ForgePlayer player : getPlayersWithStatus(status))
		{
			if (player.hasPermission(permission))
			{
				return true;
			}
		}

		return false;
	}

	public File getDataFile(String ext)
	{
		if (ext.isEmpty())
		{
			return new File(universe.getWorldDirectory(), "data/ftb_lib/teams/" + getName() + ".dat");
		}

		return new File(universe.getWorldDirectory(), "data/ftb_lib/teams/" + getName() + "." + ext + ".dat");
	}

	@Override
	public void onConfigSaved(ConfigGroup group, ICommandSender sender)
	{
		clearCache();
		markDirty();
	}
}