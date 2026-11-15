package com.af400.af400.init;

import com.af400.af400.AF400;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块注册表。对照 HeXie 的 {@code init/BlockEntitiesInit} 与 {@code blocks/*} 分区。
 *
 * <p>命名规范：全部小写下划线，按车型前缀分组，例如：
 * {@code af400_carport_side}、{@code af400_panel_wide_window}、{@code af400_head_front}。
 */
public class BlocksInit {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AF400.MOD_ID);

    // ---- 示例：占位方块，正式建模后替换 ----
    public static final DeferredBlock<Block> EXAMPLE_PART = BLOCKS.registerSimpleBlock(
            "example_part",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops()
    );

    // ---- 后续按车型分区补充 ----
    // 车体 / 侧板 / 车顶
    // 转向架
    // 驾驶室
    // 受电弓
    // 车钩
    static {
        AF400.LOGGER.debug("BlocksInit loaded");
    }
}
