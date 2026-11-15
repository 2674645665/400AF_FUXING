package com.af400.af400;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import com.af400.af400.config.AF400Config;
import com.af400.af400.init.BlockEntitiesInit;
import com.af400.af400.init.BlocksInit;
import com.af400.af400.init.CreativeTabsInit;
import com.af400.af400.init.EffectInit;
import com.af400.af400.init.EntityInit;
import com.af400.af400.init.InteractionBehaviourInit;
import com.af400.af400.init.ItemsInit;
import com.af400.af400.init.RecipeInit;
import com.af400.af400.init.SoundsInit;

/**
 * 400AF 主入口。
 *
 * <p>结构对照 HeXie：HeXie 用 {@code willow.train.hexie.Main}，
 * 这里用 {@code com.af400.af400.AF400}。注册顺序遵循 NeoForge 1.21.1 的惯例：
 * 效果 → 声音 → 方块 → 物品 → 方块实体 → 实体 → 创造标签 → 配方 → 交互行为。
 */
@Mod(AF400.MOD_ID)
public class AF400 {

    public static final String MOD_ID = "af400";

    public static final Logger LOGGER = LogUtils.getLogger();

    public AF400(IEventBus modBus, ModContainer container) {
        LOGGER.info("[400AF] 载入中……");

        // ---- 注册表 ----
        EffectInit.EFFECTS.register(modBus);
        SoundsInit.SOUNDS.register(modBus);
        BlocksInit.BLOCKS.register(modBus);
        ItemsInit.ITEMS.register(modBus);
        BlockEntitiesInit.BLOCK_ENTITIES.register(modBus);
        EntityInit.ENTITY_TYPES.register(modBus);
        CreativeTabsInit.TABS.register(modBus);
        RecipeInit.RECIPE_TYPES.register(modBus);
        RecipeInit.RECIPE_SERIALIZERS.register(modBus);

        // ---- 第三方兼容（机械动力虚影辅助建造等）----
        InteractionBehaviourInit.init();

        // ---- 生命周期 ----
        modBus.addListener(this::commonSetup);

        // ---- 配置 ----
        container.registerConfig(ModConfig.Type.COMMON, AF400Config.SPEC);

        LOGGER.info("[400AF] 载入完成。");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 跨注册表的初始化放这里
            LOGGER.info("[400AF] common setup done.");
        });
    }

    /** 便捷资源定位：af400:xxx */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
