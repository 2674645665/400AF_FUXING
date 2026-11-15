package com.af400.af400.init;

import com.af400.af400.AF400;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 音效注册表。对照 {@code assets/af400/sounds.json}。
 *
 * <p>用法：
 * <pre>{@code
 * public static final DeferredHolder<SoundEvent, SoundEvent> CHIME =
 *         SOUNDS.register("chime", () -> SoundEvent.createVariableRangeEvent(AF400.id("chime")));
 * }</pre>
 */
public class SoundsInit {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, AF400.MOD_ID);

    static {
        AF400.LOGGER.debug("SoundsInit loaded");
    }
}
