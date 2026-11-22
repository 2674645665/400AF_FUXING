package com.af400.af400.mixins.mixin;

import java.util.List;
import com.af400.af400.blocks.parts.CarriagePanelBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "willow.train.hexie.client.render.display.PassengerPanelModels", remap = false)
public abstract class PassengerDisplayBakeHeight {
    @Unique
    private static final ThreadLocal<BlockState> af400$panel = new ThreadLocal<>();

    @Inject(method = "bake", at = @At("RETURN"))
    private static void af400$checkDisplayLayouts(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult event,
                                                   org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
        int expected = 0;
        int ready = 0;
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (!(block instanceof CarriagePanelBlock)) continue;
            for (var state : block.getStateDefinition().getPossibleStates()) {
                if (!state.getValue(CarriagePanelBlock.EXTERIOR) || state.getValue(CarriagePanelBlock.PART) != 0
                        || state.getValue(CarriagePanelBlock.DISPLAY_MOUNT) == willow.train.hexie.blocks.signs.PanelDisplayMount.NONE
                        || !willow.train.hexie.blocks.signs.PanelDisplayColumn.isValid(state)) continue;
                expected++;
                var layout = willow.train.hexie.client.render.display.PassengerPanelModels.layout(state);
                if (layout != null && layout.normal().dot(Vec3.atLowerCornerOf(
                        state.getValue(CarriagePanelBlock.FACING).getNormal())) > .9) ready++;
            }
        }
        com.af400.af400.AF400.LOGGER.info("[400AF] Passenger display layouts ready: {}/{}", ready, expected);
    }

    @Inject(method = "exterior(Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;)Lwillow/train/hexie/client/render/display/PassengerPanelModels$Plane;", at = @At("HEAD"))
    private static void af400$enter(List<?> faces, BlockState state, CallbackInfoReturnable<?> callback) {
        af400$panel.set(state);
    }

    @Inject(method = "exterior(Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;)Lwillow/train/hexie/client/render/display/PassengerPanelModels$Plane;", at = @At("RETURN"))
    private static void af400$leave(List<?> faces, BlockState state, CallbackInfoReturnable<?> callback) {
        af400$panel.remove();
    }

    @ModifyArgs(method = "exterior(Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;)Lwillow/train/hexie/client/render/display/PassengerPanelModels$Plane;",
            at = @At(value = "INVOKE", target = "Lwillow/train/hexie/client/render/display/PassengerPanelModels;exterior(Ljava/util/List;Lnet/minecraft/world/phys/Vec3;I)Lwillow/train/hexie/client/render/display/PassengerPanelModels$Plane;"))
    private static void af400$faceOutside(Args args) {
        if (!CarriagePanelBlock.isExteriorPanel(af400$panel.get())) return;
        Vec3 normal = args.get(1);
        int column = args.get(2);
        args.set(1, normal.scale(-1));
        // Flipping the viewing basis must not move an already installed display to another cell.
        args.set(2, -column);
    }

    @ModifyConstant(method = "exterior(Ljava/util/List;Lnet/minecraft/world/phys/Vec3;I)Lwillow/train/hexie/client/render/display/PassengerPanelModels$Plane;", constant = @Constant(doubleValue = .25), require = 1)
    private static double af400$screenHeight(double depthFromTop) {
        return CarriagePanelBlock.passengerDisplayFromTop(depthFromTop, af400$panel.get());
    }
}
