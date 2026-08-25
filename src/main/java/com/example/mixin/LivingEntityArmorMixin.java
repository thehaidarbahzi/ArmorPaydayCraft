package com.example.mixin;

import com.example.armor.ArmorManager;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityArmorMixin {

    @Inject(at = @At("HEAD"), method = "getDamageAfterArmorAbsorb", cancellable = true)
    private void paydayArmorAbsorb(DamageSource damageSource, float damage, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof Player player)) {
            return;
        }

        if (damageSource.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }

        float remainingDamage = ArmorManager.INSTANCE.absorbDamage(player, damage);

        if (remainingDamage <= 0f) {
            cir.setReturnValue(0f);
        } else {
            cir.setReturnValue(remainingDamage);
        }
    }
}
