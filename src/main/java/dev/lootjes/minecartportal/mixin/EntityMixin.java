package dev.lootjes.minecartportal.mixin;

import dev.lootjes.minecartportal.ducks.EntityDuck;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityDuck {

    @Redirect(method = "tickNetherPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;moveToWorld(Lnet/minecraft/server/world/ServerWorld;)Lnet/minecraft/entity/Entity;"))
    public Entity captureMoveToWorld(Entity entity1, ServerWorld destination) {
        return ((EntityDuck)entity1).customMoveToWorld(destination);
    }


    @Unique
    public Entity customMoveToWorld(ServerWorld destination) {
        if (this.world instanceof ServerWorld && !this.isRemoved()) {
            this.world.getProfiler().push("changeDimension");
            List<Entity> passangers = List.copyOf(((Entity)(Object)this).getPassengerList());
            this.detach();
            this.world.getProfiler().push("reposition");
            TeleportTarget teleportTarget = this.getTeleportTarget(destination);
            if (teleportTarget == null) {
                return null;
            } else {
                this.world.getProfiler().swap("reloading");
                Entity entity = this.getType().create(destination);
                if (entity != null) {
                    entity.copyFrom((Entity)(Object)this);
                    entity.refreshPositionAndAngles(teleportTarget.position.x, teleportTarget.position.y, teleportTarget.position.z, teleportTarget.yaw, entity.getPitch());
                    entity.setVelocity(teleportTarget.velocity);
                    destination.onDimensionChanged(entity);
                    if (destination.getRegistryKey() == World.END) {
                        ServerWorld.createEndSpawnPlatform(destination);
                    }
                    for (Entity entity2 : passangers) {
                        ((EntityDuck) entity2).customMovePassengerToWorld(destination, teleportTarget);
                        entity2.startRiding(entity, true);
                    }
                }

                this.removeFromDimension();
                this.world.getProfiler().pop();
                ((ServerWorld)this.world).resetIdleTimeout();
                destination.resetIdleTimeout();
                this.world.getProfiler().pop();
                return entity;
            }
        } else {
            return null;
        }
    }

    @Shadow
    protected abstract TeleportTarget getTeleportTarget(ServerWorld destination);

    @Shadow
    protected abstract void removeFromDimension();

    @Unique
    public Entity customMovePassengerToWorld(ServerWorld destination, TeleportTarget teleportTarget) {
        if (this.world instanceof ServerWorld && !this.isRemoved()) {
            this.world.getProfiler().push("changeDimension");
            List<Entity> passangers = List.copyOf(((Entity)(Object)this).getPassengerList());
            this.detach();
            this.world.getProfiler().push("reposition");
            if (teleportTarget == null) {
                return null;
            } else {
                this.world.getProfiler().swap("reloading");
                Entity entity = this.getType().create(destination);
                if (entity != null) {
                    entity.copyFrom((Entity)(Object)this);
                    entity.refreshPositionAndAngles(teleportTarget.position.x, teleportTarget.position.y, teleportTarget.position.z, teleportTarget.yaw, entity.getPitch());
                    entity.setVelocity(teleportTarget.velocity);
                    destination.onDimensionChanged(entity);
                    if (destination.getRegistryKey() == World.END) {
                        ServerWorld.createEndSpawnPlatform(destination);
                    }
                    for (Entity entity2 : passangers) {
                        ((EntityDuck) entity2).customMovePassengerToWorld(destination, teleportTarget);
                        entity2.startRiding(entity, true);
                    }
                }

                this.removeFromDimension();
                this.world.getProfiler().pop();
                ((ServerWorld)this.world).resetIdleTimeout();
                destination.resetIdleTimeout();
                this.world.getProfiler().pop();
                return entity;
            }
        } else {
            return null;
        }
    }

    @Shadow protected abstract EntityType getType();

    @Shadow protected abstract void detach();

    @Shadow protected abstract boolean isRemoved();

    @Shadow private boolean inNetherPortal;
    @Shadow private World world;
}

