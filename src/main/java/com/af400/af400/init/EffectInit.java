package com.af400.af400.init;

import com.af400.af400.AF400;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 状态效果注册表（如车厢内晕车、过站耳鸣之类）。
 */
public class EffectInit {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, AF400.MOD_ID);

    static {
        AF400.LOGGER.debug("EffectInit loaded");
    }
}
