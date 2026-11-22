package com.af400.af400.blocks.parts;

import com.mojang.serialization.MapCodec;

/**
 * 400AF 正常小窗车厢板（1×2）。
 *
 * <p>尺寸来自 Blockbench 模型 {@code 400AF小窗1X2.bbmodel} / {@code 400AF正常小窗1X2.obj}：
 * <ul>
 *   <li>X（宽）：0 .. 1      → 1 格</li>
 *   <li>Y（高）：0 .. 2      → 2 格</li>
 *   <li>Z（厚）：0.0375 .. 0.1875 → 板厚 0.15 格</li>
 * </ul>
 *
 * <p>所以这是一块「1 格宽、2 格高、15% 格厚」的板。
 * 碰撞箱按格拆分：PART=0 是下半格、PART=1 是上半格，各自都是 1×1×0.15 的板。
 */
public class SmallWindowPanelBlock extends CarriagePanelBlock {

    public static final MapCodec<SmallWindowPanelBlock> CODEC = simpleCodec(
            props -> new SmallWindowPanelBlock(props));

    public SmallWindowPanelBlock(Properties properties) {
        super(properties, 1, 2);
    }

    @Override
    protected MapCodec<? extends SmallWindowPanelBlock> codec() {
        return CODEC;
    }
}
