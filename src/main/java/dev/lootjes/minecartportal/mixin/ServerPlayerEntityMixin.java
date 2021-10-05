package dev.lootjes.minecartportal.mixin;

import com.mojang.authlib.GameProfile;
import dev.lootjes.minecartportal.ducks.EntityDuck;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.biome.source.BiomeAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Iterator;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements EntityDuck{

    @Shadow public abstract boolean startRiding(Entity entity, boolean force);

    @Shadow @Nullable public abstract Entity moveToWorld(ServerWorld destination);

    @Shadow
    private boolean inTeleportationState;
    @Shadow
    private boolean notInAnyWorld;
    @Shadow
    private ServerPlayNetworkHandler networkHandler;
    @Shadow
    private boolean seenCredits;
    @Shadow
    private MinecraftServer server;
    @Shadow
    private ServerPlayerInteractionManager interactionManager;
    @Shadow
    private Vec3d enteredNetherPos;
    @Shadow
    private int syncedExperience;
    @Shadow
    private float syncedHealth;
    @Shadow
    private int syncedFoodLevel;

    public ServerPlayerEntityMixin(World world, BlockPos blockPos, float f, GameProfile gameProfile) {
        super(world, blockPos, f, gameProfile);
    }


    @Override
    @Unique
    public Entity customMoveToWorld(ServerWorld destination) {
        return this.moveToWorld(destination);
    }

    @Override
    @Unique
    public Entity customMovePassengerToWorld(ServerWorld destination, TeleportTarget teleportTarget) {
        this.inTeleportationState = true;
        ServerWorld serverWorld = this.getServerWorld();
        RegistryKey<World> registryKey = serverWorld.getRegistryKey();
        if (registryKey == World.END && destination.getRegistryKey() == World.OVERWORLD) {
            this.detach();
            this.getServerWorld().removePlayer((ServerPlayerEntity)(Object)this, Entity.RemovalReason.CHANGED_DIMENSION);
            if (!this.notInAnyWorld) {
                this.notInAnyWorld = true;
                this.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.GAME_WON, this.seenCredits ? 0.0F : 1.0F));
                this.seenCredits = true;
            }

            return this;
        } else {
            WorldProperties worldProperties = destination.getLevelProperties();
            this.networkHandler.sendPacket(new PlayerRespawnS2CPacket(destination.getDimension(), destination.getRegistryKey(), BiomeAccess.hashSeed(destination.getSeed()), this.interactionManager.getGameMode(), this.interactionManager.getPreviousGameMode(), destination.isDebugWorld(), destination.isFlat(), true));
            this.networkHandler.sendPacket(new DifficultyS2CPacket(worldProperties.getDifficulty(), worldProperties.isDifficultyLocked()));
            PlayerManager playerManager = this.server.getPlayerManager();
            playerManager.sendCommandTree((ServerPlayerEntity)(Object)this);
            serverWorld.removePlayer((ServerPlayerEntity)(Object)this, Entity.RemovalReason.CHANGED_DIMENSION);
            this.unsetRemoved();
            if (teleportTarget != null) {
                serverWorld.getProfiler().push("moving");
                if (registryKey == World.OVERWORLD && destination.getRegistryKey() == World.NETHER) {
                    this.enteredNetherPos = this.getPos();
                } else if (destination.getRegistryKey() == World.END) {
                    this.createEndSpawnPlatform(destination, new BlockPos(teleportTarget.position));
                }

                serverWorld.getProfiler().pop();
                serverWorld.getProfiler().push("placing");
                this.setWorld(destination);
                destination.onPlayerChangeDimension((ServerPlayerEntity)(Object)this);
                this.setRotation(teleportTarget.yaw, teleportTarget.pitch);
                this.refreshPositionAfterTeleport(teleportTarget.position.x, teleportTarget.position.y, teleportTarget.position.z);
                serverWorld.getProfiler().pop();
                this.worldChanged(serverWorld);
                this.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(this.getAbilities()));
                playerManager.sendWorldInfo((ServerPlayerEntity)(Object)this, destination);
                playerManager.sendPlayerStatus((ServerPlayerEntity)(Object)this);
                Iterator var7 = this.getStatusEffects().iterator();

                while(var7.hasNext()) {
                    StatusEffectInstance statusEffectInstance = (StatusEffectInstance)var7.next();
                    this.networkHandler.sendPacket(new EntityStatusEffectS2CPacket(this.getId(), statusEffectInstance));
                }

                this.networkHandler.sendPacket(new WorldEventS2CPacket(1032, BlockPos.ORIGIN, 0, false));
                this.syncedExperience = -1;
                this.syncedHealth = -1.0F;
                this.syncedFoodLevel = -1;
            }

            return this;
        }
    }

    @Shadow
    protected abstract void worldChanged(ServerWorld serverWorld);

    @Shadow
    protected abstract void setWorld(ServerWorld destination);

    @Shadow
    protected abstract void createEndSpawnPlatform(ServerWorld destination, BlockPos blockPos);

    @Shadow
    protected abstract ServerWorld getServerWorld();
}
