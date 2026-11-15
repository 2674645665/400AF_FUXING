package com.af400.af400.mixins.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import willow.train.hexie.blockentity.CRH2PartEntity;
import willow.train.hexie.init.BlockEntitiesInit;

import java.util.Set;

/**
 * 让 400AF 的车厢板也能挂 HeXie 的 {@code CRH2PartEntity}。
 *
 * <h3>背景</h3>
 * HeXie 的文本贴纸链路全部写死 {@code CRH2PartEntity}：
 * <pre>
 *   PanelTextItem.useOn → instanceof CRH2PartEntity
 *   PanelTextMenu       → Class.cast(CRH2PartEntity.class)
 *   CarriageMarkingMenu → Class.cast(VehicleMarkings)
 * </pre>
 * 而 {@code CRH2PartEntity} 是 {@code final}，构造器又写死了
 * {@code BlockEntitiesInit.CRH2A_PART} 这个类型。
 *
 * <h3>本 Mixin 的做法</h3>
 * 不去改 HeXie 的类结构（不去 final、不改构造器），
 * 而是<b>往 {@code CRH2A_PART} 这个 BlockEntityType 的支持方块集合里补上我们的板</b>。
 * 这样：
 * <ul>
 *   <li>我们的板可以直接用 {@code CRH2PartEntity} 作为方块实体</li>
 *   <li>`instanceof` / `Class.cast` 全部天然通过</li>
 *   <li>贴纸、水牌、界面、渲染全部复用 HeXie 的实现</li>
 * </ul>
 *
 * <h3>实现细节</h3>
 * {@code BlockEntityType.validBlocks} 是 private final，但
 * {@code getValidBlocks()} 返回的是<b>同一个 Set 实例</b>，
 * 所以直接往里 add 就能生效，无需反射或 Accessor。
 *
 * <h3>风险</h3>
 * 依赖 HeXie 的 {@code CRH2A_PART} 字段存在。若 HeXie 改名会启动报错。
 * 已计划向 HeXie 作者申请开放该实体的构造器，届时本 Mixin 可删除。
 */
@Mixin(value = BlockEntityType.class, remap = false)
public class MixinBlockEntityTypeAddBlocks {

    /**
     * 在 {@code isValid} 被调用前，把额外方块补进支持列表。
     *
     * <p>选 {@code isValid} 作为注入点是因为它是「方块能不能挂这个实体类型」的
     * 唯一判定入口；在这里补最安全，也不会漏掉任何调用路径。
     */
    @Inject(method = "isValid", at = @At("HEAD"), remap = false)
    private void af400$ensureExtraBlocks(net.minecraft.world.level.block.state.BlockState state,
                                         CallbackInfoReturnable<Boolean> cir) {
        // 只处理 HeXie 的 crh2a_part 类型
        BlockEntityType<?> self = (BlockEntityType<?>) (Object) this;
        BlockEntityType<CRH2PartEntity> target;
        try {
            target = BlockEntitiesInit.CRH2A_PART.get();
        } catch (Throwable t) {
            return;   // HeXie 还没注册好，跳过
        }
        if (self != target) {
            return;
        }

        Set<Block> extra = com.af400.af400.init.Af400ExtraBlocks.panels();
        if (extra.isEmpty()) {
            return;
        }
        Set<Block> valid = self.getValidBlocks();
        for (Block b : extra) {
            if (!valid.contains(b)) {
                valid.add(b);
            }
        }
    }
}
