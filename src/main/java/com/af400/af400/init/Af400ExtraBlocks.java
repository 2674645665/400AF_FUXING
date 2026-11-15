package com.af400.af400.init;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.List;
import java.util.Set;

/**
 * 存放「需要额外挂到 HeXie {@code crh2a_part} 实体类型上」的方块。
 *
 * <h3>为什么需要这类</h3>
 * 我们的板用 {@code CRH2PartEntity} 作为方块实体（这样贴纸/水牌/界面全复用 HeXie），
 * 但那个实体类型 {@code crh2a_part} 只登记了 HeXie 自己的方块，
 * MC 的 {@code BlockEntityType.isValid} 会拒绝我们的板。
 * {@link com.af400.af400.mixins.mixin.MixinBlockEntityTypeAddBlocks} 会把这里的方块补进去。
 *
 * <h3>为什么存 DeferredBlock 而不是 Block</h3>
 * {@code DeferredRegister} 的方块实例是延迟构建的。本类在 {@code BlocksInit} 的静态块里登记，
 * 那时 Block 实例可能还不存在，所以先存持有者，真正取用时再 {@code .get()}。
 */
public final class Af400ExtraBlocks {

    private static final List<DeferredBlock<? extends Block>> PANEL_HOLDERS =
            new java.util.ArrayList<>();

    private Af400ExtraBlocks() {
    }

    /** 由 {@link BlocksInit} 在类初始化时调用 */
    public static void registerDeferred(DeferredBlock<? extends Block>... holders) {
        for (DeferredBlock<? extends Block> h : holders) {
            PANEL_HOLDERS.add(h);
        }
    }

    /**
     * 取出所有需要补进 HeXie 实体类型的方块。
     *
     * <p>如果某个方块还没注册完（{@code .get()} 抛异常），就跳过它 ——
     * 这个集合会被反复查询，下一次查询时通常就准备好了。
     */
    public static Set<Block> panels() {
        Set<Block> out = new java.util.LinkedHashSet<>();
        for (DeferredBlock<? extends Block> h : PANEL_HOLDERS) {
            try {
                out.add(h.get());
            } catch (Throwable ignored) {
                // 尚未完成注册，跳过
            }
        }
        return out;
    }
}
