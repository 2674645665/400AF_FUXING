package com.af400.af400.mixins.mixin;

import com.af400.af400.AF400;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import willow.train.hexie.blockentity.CRH2PartEntity;
import willow.train.hexie.blocks.parts.PanelText;
import willow.train.hexie.blocks.parts.VehicleMarkingSurface;

/**
 * 【临时诊断】在 HeXie 的车厢板贴纸渲染器里打日志，用来排查
 * 「400AF 的板能贴字但看不见」的问题。
 *
 * <p>只对 400AF 的方块打日志，不影响 HeXie。
 * 每帧只打印一次（用静态计数器节流），避免刷屏。
 * 排查完就删掉这个 Mixin。
 */
@Mixin(value = willow.train.hexie.client.render.blockentity.CarriageMarkingRenderer.class, remap = false)
public class MixinMarkingRendererDebug {

    private static int af400$count = 0;

    @Inject(method = "render(Lwillow/train/hexie/blockentity/CRH2PartEntity;FLcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"), remap = false, require = 0)
    private void af400$logRender(CRH2PartEntity be, float partial,
                                 com.mojang.blaze3d.vertex.PoseStack pose,
                                 net.minecraft.client.renderer.MultiBufferSource buffer,
                                 int light, int overlay, CallbackInfo ci) {
        try {
            if (af400$count++ % 120 != 0) return;   // 约每 2 秒一次
            var block = be.getBlockState().getBlock();
            if (!(block instanceof VehicleMarkingSurface surface)) return;
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals("af400")) return;   // 只管我们自己的板

            BlockState state = be.getBlockState();
            var root = surface.markingRoot(be.getLevel(), be.getBlockPos(), state);
            PanelText t0 = be.panelText(0);
            PanelText t1 = be.panelText(1);
            AF400.LOGGER.info(
                    "[贴纸诊断] block={} pos={} part={} facing={} root={} rootEqSelf={} slots={} "
                            + "label0='{}' label1='{}' harmony=[{},{}] bounds={}",
                    id, be.getBlockPos(), state.getValue(com.af400.af400.blocks.parts.CarriagePanelBlock.PART),
                    state.getValue(com.af400.af400.blocks.parts.CarriagePanelBlock.FACING),
                    root, root.equals(be.getBlockPos()),
                    surface.markingSlots(state),
                    t0.text(), t1.text(),
                    be.harmony(0), be.harmony(1),
                    surface.markingBounds(state));
        } catch (Throwable ignored) {
            // 诊断代码绝不能影响游戏
        }
    }
}
