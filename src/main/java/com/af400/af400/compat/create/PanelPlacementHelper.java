package com.af400.af400.compat.create;

import com.af400.af400.blocks.parts.CarriagePanelBlock;
import com.af400.af400.init.BlocksInit;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.function.Predicate;

/**
 * 车厢板的机械动力「虚影辅助建造」。
 *
 * <p>对着已有车厢板再用同款车厢板时，Create 会在可延伸方向渲染绿色幽灵方块，
 * 提示下一格会拼到哪里；点一下即可连续铺设。
 *
 * <h3>延伸规则</h3>
 * <ul>
 *   <li><b>只沿板面左右方向</b>（垂直于 facing 的两个水平方向）堆叠。
 *       这是车厢板拼成一整面车体墙的自然方式。</li>
 *   <li><b>不沿前后堆叠</b>：沿着 facing 方向再拼一块，会变成里外两层板，没有意义。</li>
 *   <li><b>不上下堆叠</b>：车厢板是横排铺开的。</li>
 *   <li>只在**同一朝向**的板之间延伸，虚影保持相邻板的朝向。</li>
 *   <li>目标格必须可替换（空气/草等），已有方块则跳过。</li>
 * </ul>
 *
 * <p>实现参照 Create 的 {@code GirderPlacementHelper}。
 */
public class PanelPlacementHelper implements IPlacementHelper {

    @Override
    public Predicate<ItemStack> getItemPredicate() {
        // 所有车厢板都参与虚影辅助
        return stack -> isPanelItem(stack);
    }

    @Override
    public Predicate<BlockState> getStatePredicate() {
        return state -> isPanelBlock(state);
    }

    private static boolean isPanelItem(ItemStack stack) {
        return stack.is(BlocksInit.AF400_PANEL_SMALL_WINDOW.get().asItem())
                || stack.is(BlocksInit.AF400_PANEL_WIDE_WINDOW.get().asItem())
                || stack.is(BlocksInit.AF400_PANEL_NO_WINDOW.get().asItem());
    }

    private static boolean isPanelBlock(BlockState state) {
        return state.is(BlocksInit.AF400_PANEL_SMALL_WINDOW.get())
                || state.is(BlocksInit.AF400_PANEL_WIDE_WINDOW.get())
                || state.is(BlocksInit.AF400_PANEL_NO_WINDOW.get());
    }

    @Override
    public PlacementOffset getOffset(Player player, Level level, BlockState state,
                                     BlockPos pos, BlockHitResult ray) {
        // 只在主体格（PART=0，也就是下半格）上给虚影
        if (state.getValue(CarriagePanelBlock.PART) != 0) {
            return PlacementOffset.fail();
        }

        if (!(state.getBlock() instanceof CarriagePanelBlock panel)) {
            return PlacementOffset.fail();
        }

        Direction facing = state.getValue(CarriagePanelBlock.FACING);
        int width = panel.widthCells();      // 1 或 2
        int height = panel.heightCells();    // 通常是 2

        Direction right = facing.getClockWise();
        Direction left = facing.getCounterClockWise();

        // 关键：新板要**整体**挪开 width 格，不是 1 格。
        //
        // 宽客板宽 2，如果只挪 1 格，新板的占位格会和旧板重叠（旧板占 0..1，
        // 新板占 1..2），永远放不下 —— 现象就是「虚影消失了」。
        // 所以要沿 board 的宽度方向整体平移 width 格：
        //   向右 -> 新板占 pos + right*width .. pos + right*(2*width-1)
        //   向左 -> 新板从 pos - left*width 开始
        BlockPos rightTarget = pos.relative(right, width);
        BlockPos leftTarget = pos.relative(left, width);

        List<Direction> order = IPlacementHelper.orderedByDistance(pos, ray.getLocation(),
                dir -> dir == right || dir == left);

        for (Direction dir : order) {
            BlockPos target = (dir == right) ? rightTarget : leftTarget;
            if (canPlaceAt(level, target, right, width, height)) {
                return ghost(target, facing);
            }
        }

        return PlacementOffset.fail();
    }

    /**
     * 检查以 {@code target} 为主体格放一块板时，{@code width × height} 格是否都空着。
     *
     * <p>占位格布局与 {@code CarriagePanelBlock.offsetOf} 保持一致：
     * 主体在 (0,0)，沿 {@code right} 方向铺 col 格，向上铺 row 格。
     */
    private static boolean canPlaceAt(Level level, BlockPos target, Direction right,
                                      int width, int height) {
        for (int col = 0; col < width; col++) {
            for (int row = 0; row < height; row++) {
                BlockPos p = target.relative(right, col).above(row);
                if (!level.getBlockState(p).canBeReplaced()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 生成一个「保持朝向、PART 归零（下半格）」的幽灵状态 */
    private static PlacementOffset ghost(BlockPos target, Direction facing) {
        return PlacementOffset.success(target, s -> s
                .setValue(CarriagePanelBlock.FACING, facing)
                .setValue(CarriagePanelBlock.PART, 0));
    }
}
