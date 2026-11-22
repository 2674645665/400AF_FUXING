package com.af400.af400.mixins.mixin;

import com.af400.af400.blocks.parts.CarriagePanelBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import willow.train.hexie.blocks.signs.PanelDisplayColumn;

@Mixin(targets = "willow.train.hexie.blocks.signs.PanelAttachmentSurface", remap = false)
public abstract class PassengerDisplayControlHeight {
    @Redirect(method = "build", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Direction;getOpposite()Lnet/minecraft/core/Direction;"))
    private static Direction af400$outside(Direction facing, BlockState state) {
        return CarriagePanelBlock.isExteriorPanel(state) ? facing : facing.getOpposite();
    }

    @Redirect(method = "build", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Direction;getClockWise()Lnet/minecraft/core/Direction;"))
    private static Direction af400$screenRight(Direction facing, BlockState state) {
        return CarriagePanelBlock.isExteriorPanel(state) ? facing.getCounterClockWise() : facing.getClockWise();
    }

    @Redirect(method = "build", at = @At(value = "INVOKE", target = "Lwillow/train/hexie/blocks/signs/PanelDisplayColumn;offset()I"))
    private static int af400$screenColumn(PanelDisplayColumn column, BlockState state) {
        return CarriagePanelBlock.isExteriorPanel(state) ? -column.offset() : column.offset();
    }

    @ModifyConstant(method = "build", constant = @Constant(doubleValue = .25), require = 2)
    private static double af400$screenHeight(double original, BlockState state) {
        return CarriagePanelBlock.passengerDisplayFromTop(original, state);
    }
}
