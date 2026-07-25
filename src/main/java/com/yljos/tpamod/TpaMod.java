package com.yljos.tpamod;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mod(
        modid = TpaMod.MODID,
        name = TpaMod.NAME,
        version = TpaMod.VERSION,
        acceptableRemoteVersions = "*" // Allow clients to join without installing this mod
)
public class TpaMod {
    public static final String MODID = "tpamod";
    public static final String NAME = "Tpa Mod";
    public static final String VERSION = "1.0";

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandTpa());
        event.registerServerCommand(new CommandSetHome());
        event.registerServerCommand(new CommandHome());
    }

    // Play teleport effects
    public static void playTeleportEffects(EntityPlayerMP player) {
        WorldServer world = player.getServerWorld();
        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
        world.spawnParticle(EnumParticleTypes.PORTAL, player.posX, player.posY + 1.0D, player.posZ, 32, 0.5D, 1.0D, 0.5D, 0.1D);
    }
}

class CommandTpa extends CommandBase {
    @Override
    public String getName() { return "tpa"; }

    @Override
    public String getUsage(ICommandSender sender) { return "/tpa <player>"; }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true; // Force allow all players
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) return;
        if (args.length < 1) throw new WrongUsageException(getUsage(sender));

        EntityPlayerMP player = (EntityPlayerMP) sender;
        EntityPlayerMP target = getPlayer(server, sender, args[0]);

        TpaMod.playTeleportEffects(player); // Source effects

        if (player.dimension != target.dimension) {
            server.getPlayerList().transferPlayerToDimension(player, target.dimension, new ITeleporter() {
                @Override
                public void placeEntity(World world, Entity entity, float yaw) {
                    entity.setLocationAndAngles(target.posX, target.posY, target.posZ, entity.rotationYaw, entity.rotationPitch);
                }
            });
        } else {
            player.connection.setPlayerLocation(target.posX, target.posY, target.posZ, player.rotationYaw, player.rotationPitch);
        }

        TpaMod.playTeleportEffects(player); // Destination effects
        player.sendMessage(new TextComponentString("Teleported to " + target.getName()));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            List<String> players = new ArrayList<>(Arrays.asList(server.getOnlinePlayerNames()));
            players.remove(sender.getName()); // Remove self from autocomplete list
            return getListOfStringsMatchingLastWord(args, players);
        }
        return Collections.emptyList();
    }
}

class CommandSetHome extends CommandBase {
    @Override
    public String getName() { return "sethome"; }

    @Override
    public String getUsage(ICommandSender sender) { return "/sethome"; }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true; // Force allow all players
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) sender;
        NBTTagCompound persisted = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);

        NBTTagCompound homeData = new NBTTagCompound();
        homeData.setDouble("x", player.posX);
        homeData.setDouble("y", player.posY);
        homeData.setDouble("z", player.posZ);
        homeData.setInteger("dim", player.dimension);

        persisted.setTag("HomeLocation", homeData);
        player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted); // Persist through death

        player.sendMessage(new TextComponentString("Home location set successfully."));
    }
}

class CommandHome extends CommandBase {
    @Override
    public String getName() { return "home"; }

    @Override
    public String getUsage(ICommandSender sender) { return "/home"; }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true; // Force allow all players
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) sender;
        NBTTagCompound persisted = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);

        if (!persisted.hasKey("HomeLocation")) {
            player.sendMessage(new TextComponentString("You have not set a home yet. Use /sethome."));
            return;
        }

        NBTTagCompound homeData = persisted.getCompoundTag("HomeLocation");
        double x = homeData.getDouble("x");
        double y = homeData.getDouble("y");
        double z = homeData.getDouble("z");
        int dim = homeData.getInteger("dim");

        TpaMod.playTeleportEffects(player); // Source effects

        if (player.dimension != dim) {
            server.getPlayerList().transferPlayerToDimension(player, dim, new ITeleporter() {
                @Override
                public void placeEntity(World world, Entity entity, float yaw) {
                    entity.setLocationAndAngles(x, y, z, entity.rotationYaw, entity.rotationPitch);
                }
            });
        } else {
            player.connection.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);
        }

        TpaMod.playTeleportEffects(player); // Destination effects
        player.sendMessage(new TextComponentString("Teleported home."));
    }
}