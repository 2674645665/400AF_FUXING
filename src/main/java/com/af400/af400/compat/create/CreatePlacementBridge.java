package com.af400.af400.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.lang.reflect.Method;

/**
 * 车厢板 → Create 虚影放置的<b>反射桥</b>。
 *
 * <p>为什么用反射：{@code CarriagePanelBlock} 是所有车厢板共用的基类，
 * 它一定会在没装 Create 的环境里被加载。如果它直接 import
 * {@code net.createmod.catnip.placement.IPlacementHelper}，
 * 那么 JVM 在链接这个类时就会抛 {@code NoClassDefFoundError}，
 * 整个 mod 直接崩。
 *
 * <p>所以这里只持有 {@code Object} 和 {@code Method}，所有 Create 类型
 * 都在 {@link CreateCompat} 里间接处理，加载链路是：
 * <pre>
 *   CarriagePanelBlock.useItemOn
 *     → CreatePlacementBridge.tryPlace（本类，无 Create 引用）
 *       → CreateCompat.panelHelper()（有 Create 引用，仅 create 存在时初始化）
 * </pre>
 */
public final class CreatePlacementBridge {

    private CreatePlacementBridge() {
    }

    /**
     * 尝试用 Create 的放置助手放置。
     *
     * @return 成功放置时返回对应结果；无法处理（未装 Create / 不匹配 / 没有落点）时返回 {@code null}，
     *         调用方继续走原版逻辑
     */
    public static ItemInteractionResult tryPlace(Block block, ItemStack stack, BlockState state,
                                                 Level level, BlockPos pos, Player player,
                                                 InteractionHand hand, BlockHitResult hit) {
        if (!CreateCompat.isRegistered()) {
            return null;
        }
        try {
            Object helper = CreateCompat.panelHelper();
            if (helper == null) {
                return null;
            }
            Class<?> helperIface = helper.getClass().getInterfaces().length > 0
                    ? helper.getClass().getInterfaces()[0]
                    : Class.forName("net.createmod.catnip.placement.IPlacementHelper");

            // matchesItem / matchesState
            Method matchesItem = helperIface.getMethod("matchesItem", ItemStack.class);
            Method matchesState = helperIface.getMethod("matchesState", BlockState.class);
            if (!(boolean) matchesItem.invoke(helper, stack)) return null;
            if (!(boolean) matchesState.invoke(helper, state)) return null;

            // getOffset(player, level, state, pos, hit)
            Method getOffset = helperIface.getMethod("getOffset", Player.class, Level.class,
                    BlockState.class, BlockPos.class, BlockHitResult.class);
            Object offset = getOffset.invoke(helper, player, level, state, pos, hit);
            if (offset == null) return null;

            Class<?> offsetClass = Class.forName("net.createmod.catnip.placement.PlacementOffset");
            if (!(boolean) offsetClass.getMethod("isSuccessful").invoke(offset)) {
                return null;
            }

            // placeInWorld(level, BlockItem, player, hand, hit)
            Method placeInWorld = offsetClass.getMethod("placeInWorld",
                    Level.class, BlockItem.class, Player.class, InteractionHand.class, BlockHitResult.class);
            BlockItem blockItem = stack.getItem() instanceof BlockItem bi ? bi : null;
            Object result = placeInWorld.invoke(offset, level, blockItem, player, hand, hit);
            return result instanceof ItemInteractionResult ir ? ir : null;
        } catch (Throwable t) {
            // 兼容层任何异常都不应该让游戏崩，降级成普通放置
            com.af400.af400.AF400.LOGGER.warn("[400AF] Create 虚影放置失败，降级为普通放置: {}", t.toString());
            return null;
        }
    }
}
