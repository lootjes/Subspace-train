package dev.lootjes.minecartportal.ducks;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.TeleportTarget;

public interface EntityDuck {
    Entity customMoveToWorld(ServerWorld destination);
    Entity customMovePassengerToWorld(ServerWorld destination, TeleportTarget teleportTarget);
}
