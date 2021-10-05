package dev.lootjes.minecartportal.mixin;

import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {

    @Redirect(method = "onEntityCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;hasPassengers()Z"))
    public boolean hasPassengers(Entity entity) {
        return false;
    }
}
