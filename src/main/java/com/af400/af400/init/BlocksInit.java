package com.af400.af400.init;

import com.af400.af400.AF400;
import com.af400.af400.blocks.parts.NoWindowPanelBlock;
import com.af400.af400.blocks.parts.SmallWindowPanelBlock;
import com.af400.af400.blocks.parts.WidePanelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块注册表。对照 HeXie 的 {@code init/BlockInit*} 与 {@code blocks/*} 分区。
 *
 * <p>命名规范：全部小写下划线，按车型前缀分组，例如：
 * {@code af400_carport_side}、{@code af400_panel_wide_window}、{@code af400_head_front}。
 */
public class BlocksInit {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AF400.MOD_ID);

    // ------------------------------------------------------------------
    // 车厢板
    // ------------------------------------------------------------------

    /**
     * 400AF 正常小窗车厢板（1 格宽 × 2 格高，板厚 0.15 格）。
     *
     * <p>模型：{@code assets/af400/models/block/af400/parts/af400_panel_small_window.obj}
     * <p>碰撞：两格板状，见 {@link SmallWindowPanelBlock}
     */
    public static final DeferredBlock<SmallWindowPanelBlock> AF400_PANEL_SMALL_WINDOW =
            BLOCKS.register("af400_panel_small_window",
                    () -> new SmallWindowPanelBlock(panelProperties()));

    /**
     * 400AF 宽客车厢板（2 格宽 × 2 格高，板厚 0.15 格）。
     *
     * <p>模型：{@code assets/af400/models/block/af400/parts/af400_panel_wide_window.obj}
     * <p>碰撞：4 格板状（PART 0..3），见 {@link WidePanelBlock}
     */
    public static final DeferredBlock<WidePanelBlock> AF400_PANEL_WIDE_WINDOW =
            BLOCKS.register("af400_panel_wide_window",
                    () -> new WidePanelBlock(panelProperties()));

    /**
     * 400AF 无窗车厢板（1 格宽 × 2 格高，板厚 0.15 格）。
     *
     * <p>模型：{@code assets/af400/models/block/af400/parts/af400_panel_no_window.obj}
     * <p>单层模型（贴图无半透明，不需要 composite）。
     */
    public static final DeferredBlock<NoWindowPanelBlock> AF400_PANEL_NO_WINDOW =
            BLOCKS.register("af400_panel_no_window",
                    () -> new NoWindowPanelBlock(panelProperties()));

    /** 车厢板通用方块属性 */
    private static BlockBehaviour.Properties panelProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GRAY)
                .strength(1.5F, 6.0F)
                .sound(SoundType.METAL)
                .noOcclusion()                    // 板状，不挡光
                .isViewBlocking((s, l, p) -> false)
                .requiresCorrectToolForDrops()
                .pushReaction(PushReaction.BLOCK);
    }

    // ---- 后续按车型分区补充 ----
    // 车体 / 车顶 / 转向架 / 驾驶室 / 受电弓 / 车钩
    static {
        AF400.LOGGER.debug("BlocksInit loaded");
        // 把三块板登记进「需要用 HeXie CRH2PartEntity 的类型」集合。
        // 注意：必须在方块对象已经创建后调用；DeferredRegister 的 supplier 是延迟执行的，
        // 所以这里只登记 DeferredBlock 的持有者，真正的 Block 实例在 Mixin 里通过 .get() 获取。
        Af400ExtraBlocks.registerDeferred(
                AF400_PANEL_SMALL_WINDOW, AF400_PANEL_WIDE_WINDOW, AF400_PANEL_NO_WINDOW);
    }
}
