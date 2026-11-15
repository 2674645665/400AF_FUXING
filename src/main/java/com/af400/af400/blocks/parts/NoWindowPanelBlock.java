package com.af400.af400.blocks.parts;

import com.mojang.serialization.MapCodec;

/**
 * 400AF 无窗车厢板（1 格宽 × 2 格高，板厚 0.15 格）。
 *
 * <p>尺寸来自 Blockbench 模型 {@code 400AF无窗客车板.bbmodel}：
 * <ul>
 *   <li>X（宽）：0 .. 1      → 1 格</li>
 *   <li>Y（高）：0 .. 2      → 2 格</li>
 *   <li>Z（厚）：0.0375 .. 0.1875 → 板厚 0.15 格</li>
 * </ul>
 *
 * <p>与 {@link SmallWindowPanelBlock} 的区别：贴图里没有半透明窗户区，
 * 所以模型是**单层**的（不需要 composite 拆 body/glass）。
 * 碰撞逻辑完全一致 —— 同样是 2 格板状。
 */
public class NoWindowPanelBlock extends CarriagePanelBlock {

    public static final MapCodec<NoWindowPanelBlock> CODEC = simpleCodec(NoWindowPanelBlock::new);

    /** 板厚：z 0.6/16 .. 3.0/16 */
    private static final float DEPTH_MIN = 0.6f / 16.0f;
    private static final float DEPTH_MAX = 3.0f / 16.0f;

    public NoWindowPanelBlock(Properties properties) {
        super(properties, 1, 2, DEPTH_MIN, DEPTH_MAX);
    }

    @Override
    protected MapCodec<? extends NoWindowPanelBlock> codec() {
        return CODEC;
    }
}
