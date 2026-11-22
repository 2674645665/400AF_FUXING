package com.af400.af400.mixins.mixin;

import com.af400.af400.blocks.parts.CarriagePanelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(targets = "willow.train.hexie.blocks.signs.PanelAttachmentHit", remap = false)
public abstract class PassengerDisplayHitHeight {
    @Inject(method = "exterior", at = @At("HEAD"), cancellable = true)
    private static void af400$outsideHit(BlockState state, BlockHitResult hit, CallbackInfoReturnable<Boolean> callback) {
        if (CarriagePanelBlock.isExteriorPanel(state)) {
            callback.setReturnValue(hit.getDirection() == state.getValue(CarriagePanelBlock.FACING));
        }
    }

    @ModifyConstant(method = "resolve", constant = {@Constant(doubleValue = 2.2), @Constant(doubleValue = 5.9)}, require = 2)
    private static double af400$screenSide(double original, BlockGetter level, BlockPos pos) {
        // Keep the native column projection, but mirror the screen offsets with its viewing basis.
        return CarriagePanelBlock.isExteriorPanel(level.getBlockState(pos)) ? -original : original;
    }

    @ModifyConstant(method = "resolve", constant = @Constant(doubleValue = .25), require = 1)
    private static double af400$screenHeight(double original, BlockGetter level, BlockPos pos) {
        return CarriagePanelBlock.passengerDisplayFromTop(original, level.getBlockState(pos));
    }
}
