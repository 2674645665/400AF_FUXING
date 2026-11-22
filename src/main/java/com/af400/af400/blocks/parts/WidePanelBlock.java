package com.af400.af400.blocks.parts;

import com.mojang.serialization.MapCodec;

/**
 * 400AF 宽客车厢板（2 格宽 × 2 格高）。
 *
 * <p>尺寸来自 Blockbench 模型 {@code 400AF宽客车板.bbmodel}：
 * <ul>
 *   <li>X（宽）：0 .. 2      → 2 格</li>
 *   <li>Y（高）：0 .. 2      → 2 格</li>
 *   <li>Z（厚）：0.0375 .. 0.1875 → 板厚 0.15 格</li>
 * </ul>
 *
 * <p>占位格布局（PART 0..3，宽度向右铺、高度向上铺）：
 * <pre>
 *   2 3     上排
 *   0 1     下排
 * </pre>
 * 其中 PART=0 是主体（放模型、渲染、掉物品）。
 *
 * <p>注意：源模型导出时 X 是 -0.5..1.5（以中线建模），
 * 已由 {@code tools/shift-model.ps1} 平移到 0..2，见工程里的 OBJ。
 */
public class WidePanelBlock extends CarriagePanelBlock {

    public static final MapCodec<WidePanelBlock> CODEC = simpleCodec(WidePanelBlock::new);

    public WidePanelBlock(Properties properties) {
        super(properties, 2, 2);
    }

    @Override
    protected MapCodec<? extends WidePanelBlock> codec() {
        return CODEC;
    }
}
