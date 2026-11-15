package com.af400.af400.blocks.parts;

import com.af400.af400.AF400;
import com.af400.af400.init.BlockEntitiesInit;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * 车厢板方块基类 —— 多格占位 + 板状碰撞箱。
 *
 * <p>设计要点：
 * <ul>
 *   <li>{@code PART} 属性 0..15，用来标记这块是「整块部件的第几格」。
 *       只有 {@code PART=0} 是主体（放模型、发声、掉物品），其余格是纯占位。</li>
 *   <li>碰撞箱由子类给出的「每格形状」决定。车厢板是 1×2×0.15 的板，
 *       所以 PART=0 占满下半格的板厚，PART=1 占上半格的板厚。</li>
 *   <li>破坏任意一格会连带拆掉整块。</li>
 * </ul>
 *
 * <p>坐标约定（参照 HeXie）：
 * 部件在世界的占地范围是 {@code bounds = [0,0,0, width, height, depth]}，
 * 其中 X 沿 facing 的垂直方向铺开、Y 向上、Z 是板厚方向。
 *
 * <h3>与 HeXie 的对接</h3>
 * 实现 {@link PartStructure}，让 400AF 的车厢板能被 HeXie 当作「零件结构」识别。
 * 这是 HeXie 的电子水牌 / 车厢号显示屏能装到本模组板面附近的<b>前提</b> ——
 * {@code PassengerDisplayInstallation.install()} 会先调用
 * {@code PartStructures.resolve()}，要求方块是 {@code PartStructure}。
 *
 * <p>注意：显示牌本身是 <b>HeXie 自己的独立方块</b>
 * <h3>与 HeXie 的对接</h3>
 * 让 400AF 的车厢板能被 HeXie 的电子水牌 / 车厢号显示屏直接贴在板面上。
 *
 * <p>HeXie 的 {@code PassengerDisplayPanel} 接口设计得很好：只要方块 implements 它，
 * 以下全部<b>自动获得</b>（都是接口的 default 实现）：
 * <ul>
 *   <li>{@code supportsPassengerDisplay()} 默认返回 true</li>
 *   <li>{@code newBlockEntity()} 默认返回 HeXie 的 {@code PassengerPanelEntity}，
 *       它已经处理了数据源、缓存、NBT、网络同步</li>
 *   <li>渲染由 HeXie 的 {@code PassengerPanelModels} 接管</li>
 * </ul>
 *
 * <p><b>不需要</b>给板加 display_column / display_mount 属性 ——
 * 那两个属性属于 HeXie 自己的显示牌方块，板材上不适用。
 * （一度加过，导致 blockstate 变体数涨到 1344，已撤销。）
 *
 * <p>此外还要实现 {@link PartStructure}，因为 HeXie 需要知道
 * 「这块板占哪几格」，才能把显示牌挂到正确的格上。
 */
public abstract class CarriagePanelBlock extends HorizontalDirectionalBlock
        implements willow.train.hexie.blocks.parts.PartStructure,
                   willow.train.hexie.blocks.signs.PassengerDisplayPanel,
                   willow.train.hexie.blocks.parts.VehicleMarkingSurface {

    /** facing 属性 */
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /**
     * 本格装了哪种显示牌（无 / 车号屏 / 水牌 / 两者都装）。
     *
     * <p><b>必须加</b>：HeXie 的 {@code PassengerDisplayInstallation.install()}
     * 会直接 {@code state.getValue(PanelDisplayMount.PROPERTY)}，
     * 缺这个属性会抛 {@code IllegalArgumentException} 并崩游戏。
     *
     * <p>因此直接复用 HeXie 的属性对象，让读写保持同一份。
     * 代价：blockstate 变体数 = 4 朝向 × 16 part × 7 mount = 448。
     */
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<
            willow.train.hexie.blocks.signs.PanelDisplayMount> DISPLAY_MOUNT =
            willow.train.hexie.blocks.signs.PanelDisplayMount.PROPERTY;

    /**
     * 显示牌在本板上的横向位置（根 / 左 / 右）。
     *
     * <p><b>同样必须加</b>：HeXie 的 {@code install()} 在写完 displays 之后，
     * 会继续 {@code setValue(PanelDisplayColumn.PROPERTY, ...)} 来标记水牌落在板的哪一列。
     * 缺它就抛 {@code IllegalArgumentException}（实测崩溃点 PassengerDisplayInstallation.java:58）。
     *
     * <p>两者叠起来后 blockstate 变体数 = 4 朝向 × 16 part × 7 displays × 3 column = <b>1344</b>，
     * 单文件约 160 KB。这是能贴水牌必须付的代价。
     */
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<
            willow.train.hexie.blocks.signs.PanelDisplayColumn> DISPLAY_COLUMN =
            willow.train.hexie.blocks.signs.PanelDisplayColumn.PROPERTY;

    /**
     * 这块是部件的第几格（0 = 主体）。
     *
     * <p>范围固定为 0..15，是为了让同一套 blockstate 生成逻辑能覆盖所有板型；
     * 具体每种板只会用到 {@code 0 .. partCount()-1}，其余值是空模型占位。
     *
     * <p>之所以不按每种板动态收窄：{@code PART} 是 {@code static} 的，
     * 多个子类共享同一个属性对象，按类型变化会导致 BlockState 定义冲突。
     */
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 15);

    /** 每格的宽度（格为单位，1.0 = 一格） */
    private final int widthCells;
    /** 每格的高度（格为单位） */
    private final int heightCells;
    /** 板厚下限（0..1，相对本格 Z） */
    private final float depthMin;
    /** 板厚上限（0..1，相对本格 Z） */
    private final float depthMax;

    protected CarriagePanelBlock(Properties properties, int widthCells, int heightCells,
                                 float depthMin, float depthMax) {
        super(properties);
        this.widthCells = widthCells;
        this.heightCells = heightCells;
        this.depthMin = depthMin;
        this.depthMax = depthMax;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, 0)
                .setValue(DISPLAY_MOUNT, willow.train.hexie.blocks.signs.PanelDisplayMount.NONE)
                .setValue(DISPLAY_COLUMN, willow.train.hexie.blocks.signs.PanelDisplayColumn.ROOT));
    }

    @Override
    protected abstract MapCodec<? extends HorizontalDirectionalBlock> codec();

    // ------------------------------------------------------------------
    // 部件尺寸
    // ------------------------------------------------------------------

    /** 部件总格数（宽 × 高） */
    public int partCount() {
        return widthCells * heightCells;
    }

    public int widthCells() {
        return widthCells;
    }

    public int heightCells() {
        return heightCells;
    }

    // ------------------------------------------------------------------
    // 方块实体
    // ------------------------------------------------------------------

    /**
     * 直接返回 HeXie 的 {@code CRH2PartEntity}。
     *
     * <p>这是「让 400AF 复用 HeXie 全套贴纸/水牌」的关键：
     * 该类同时实现了 {@code VehicleMarkings}（文本贴纸）、
     * {@code PassengerDisplayHost}（电子水牌/车号屏）、{@code MenuProvider}（编辑界面入口），
     * 并且 HeXie 的 {@code PanelTextItem} / {@code PanelTextMenu} 会做
     * {@code instanceof / Class.cast(CRH2PartEntity.class)} —— 用它才能全部通过。
     *
     * <p>配套：{@link com.af400.af400.mixins.mixin.MixinBlockEntityTypeAddBlocks}
     * 会把本模组的板补进 {@code crh2a_part} 的合法方块集合，
     * 否则 {@code BlockEntityType.isValid} 会拒绝。
     */
    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new willow.train.hexie.blockentity.CRH2PartEntity(pos, state);
    }

    // ------------------------------------------------------------------
    // HeXie VehicleMarkingSurface 对接（车厢板文本贴纸）
    // ------------------------------------------------------------------

    /** 贴纸挂在部件的主体格上 */
    @Override
    public BlockPos markingRoot(net.minecraft.world.level.BlockGetter level, BlockPos pos, BlockState state) {
        return rootOf(pos, state);
    }

    /**
     * 贴纸可写的区域（局部坐标，单格为基准）。
     *
     * <p>返回的是「一格」的范围 {@code 0..1 × 0..1}，而不是整块板。
     * 原因：HeXie 的渲染器以 {@code translate(0.5,0,0.5)} 作为旋转中心，
     * 即假定贴纸坐标系是单格的。返回 0..2 会让宽板的文字位置整体偏移。
     * 多列（宽板 2 列）由渲染器自己按 {@code markingSlots} 逐列偏移。
     *
     * <p>Z 用本格自身的板厚范围。
     */
    @Override
    public net.minecraft.world.phys.AABB markingBounds(BlockState state) {
        return Block.box(
                0.0, 0.0, depthMin * 16.0,
                16.0, 16.0, depthMax * 16.0).bounds();
    }

    /** 渲染边界与可写区域相同 */
    @Override
    public net.minecraft.world.phys.AABB markingRenderBounds(BlockState state) {
        return markingBounds(state);
    }

    /**
     * 文字沿板面法线方向的 Z 坐标（相对本方块的局部坐标）。
     *
     * <p><b>注意参数语义</b>：HeXie 渲染器调用它时传进来的是文字的 <b>Y 坐标</b>
     * （见 {@code drawLabel} 里的 {@code surface.markingDepth(y)}），
     * 不是点击深度。
     *
     * <p>所以这里<b>不能用</b> {@code max(param, depthMax)} 之类的写法 ——
     * 那样 y 越大 Z 越大，文字会整个斜着飘出板外（实测的「歪」就是这个原因）。
     * 正确做法是：始终把文字放在板面外侧一个固定偏移处。
     */
    @Override
    public double markingDepth(double ignored) {
        return depthMax + 0.002;
    }

    // ------------------------------------------------------------------
    // HeXie VehicleMarkingSurface 对接结束
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // HeXie PartStructure 对接
    // ------------------------------------------------------------------

    /**
     * 返回本部件的主体位置。
     *
     * <p>HeXie 用这个结果去找「零件的根」，水牌安装时会挂到根格上。
     */
    @Override
    public BlockPos partRoot(net.minecraft.world.level.BlockGetter level, BlockPos pos, BlockState state) {
        return rootOf(pos, state);
    }

    /**
     * 返回整块部件占据的所有格子及其状态。
     *
     * <p>HeXie 用它判断「这个位置属于哪个零件」、把显示牌挂到正确的格上。
     *
     * <p><b>重要</b>：HeXie 的 {@code PanelDisplayColumn.isValid()} 会传入
     * {@code EmptyBlockGetter.INSTANCE}（空世界）来调用本方法。
     * 因此这里**不能**靠 {@code level.getBlockState(p)} 过滤 ——
     * 空世界里所有格子都是空气，过滤后返回空 Map，显示牌就永远装不上。
     * 直接按部件几何生成即可。
     */
    @Override
    public java.util.Map<BlockPos, BlockState> partCells(net.minecraft.world.level.BlockGetter level,
                                                         BlockPos pos, BlockState state) {
        java.util.Map<BlockPos, BlockState> cells = new java.util.LinkedHashMap<>();
        Direction facing = state.getValue(FACING);
        BlockPos root = rootOf(pos, state);
        for (int part = 0; part < partCount(); part++) {
            BlockPos p = worldPosOf(root, part, facing);
            // 用部件自身的状态（保持朝向），不查世界
            cells.put(p, state.setValue(PART, part));
        }
        return cells;
    }

    // ------------------------------------------------------------------
    // HeXie PartStructure 对接结束
    // ------------------------------------------------------------------

    /**
     * 第 {@code part} 格相对主体的偏移（沿 facing 的右方 = 宽，向上 = 高）。
     */
    public BlockPos offsetOf(int part) {
        if (part <= 0) return BlockPos.ZERO;
        int col = part % widthCells;          // 沿宽度方向的第几列
        int row = part / widthCells;          // 向上第几行
        // 主体在第 0 列第 0 行；宽度向右铺，高度向上铺
        return new BlockPos(col, row, 0);
    }

    /**
     * 主体位置（从任意一格反推）。
     *
     * <p>注意：不能用 {@code pos.offset(-col, -row, 0)} —— {@link #offsetOf} 的 X 分量
     * 表示「沿 facing 右方第几列」，不是世界 X。facing 为 EAST/WEST 时右方是 ±Z，
     * 直接减到 X 上会算出错误的位置（曾导致朝西放置的板失去碰撞）。
     */
    public BlockPos rootOf(BlockPos pos, BlockState state) {
        int part = state.getValue(PART);
        if (part <= 0) return pos;
        Direction facing = state.getValue(FACING);
        int col = part % widthCells;
        int row = part / widthCells;
        // 反推：把「右方 col 格、上 row 格」从当前位置减回去
        return pos.relative(facing.getClockWise(), -col).below(row);
    }

    // ------------------------------------------------------------------
    // 形状：板状
    // ------------------------------------------------------------------

    /**
     * 单格的板状碰撞箱。
     *
     * <p>厚度沿 Z 轴铺开，宽度沿 X 轴铺开，高度沿 Y 轴铺开。
     * 由于 {@code HorizontalDirectionalBlock} 的朝向由模型自己处理，
     * 这里统一用 NORTH（-Z 为外侧）计算，再按 facing 旋转。
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state.getValue(FACING));
    }

    /**
     * 单格碰撞箱 —— 板状。
     *
     * <p>不手工推导旋转，而是**按朝向直接指定窄条贴在哪条边**，避免 90°/180°/270°
     * 的旋转方向搞错（之前就是这样导致朝北放置没有碰撞）。
     *
     * <p>坐标依据（OBJ 实测）：板厚落在 z = 0.0375..0.1875 格 = 0.6..3.0（16 分之一格），
     * 紧贴 z=0 边界。所以 {@code facing=NORTH}、模型 {@code y=0} 时板贴 -Z 边。
     */
    private VoxelShape shapeFor(Direction facing) {
        double a = depthMin * 16.0;   // 0.6
        double b = depthMax * 16.0;   // 3.0
        double lo = a, hi = b;        // 窄条在坐标轴上的区间（距 0 边界）
        double nlo = 16.0 - b, nhi = 16.0 - a;   // 同一条边贴另一头

        return switch (facing) {
            // 板贴 -Z 边（北）
            case NORTH -> Block.box(0.0, 0.0, lo, 16.0, 16.0, hi);
            // 板贴 +Z 边（南）
            case SOUTH -> Block.box(0.0, 0.0, nlo, 16.0, 16.0, nhi);
            // 板贴 -X 边（西）
            case WEST -> Block.box(lo, 0.0, 0.0, hi, 16.0, 16.0);
            // 板贴 +X 边（东）
            case EAST -> Block.box(nlo, 0.0, 0.0, nhi, 16.0, 16.0);
            default -> Block.box(0.0, 0.0, lo, 16.0, 16.0, hi);
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // 只有主体格（PART=0）渲染模型，占位格不渲染。
        //
        // 注意：占位格曾经试过也返回 MODEL（靠 empty.json 隐藏），
        // 结果 composite 模型里的半透明玻璃层被遮挡/深度冲突，窗户变成纯黑。
        // 所以这里保持 INVISIBLE —— 占位格的碰撞和破坏由 onRemove 连带处理，
        // 不依赖它自己是否可拾取。
        return state.getValue(PART) == 0 ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    // ------------------------------------------------------------------
    // 放置：一次放下整块
    // ------------------------------------------------------------------

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // 板要贴在「玩家看着的那一面」。
        //
        // 模型约定：facing=NORTH 且 y=0 时，板贴在方块的 -Z 侧（北边）；
        // facing=SOUTH 时旋转 180°，板贴到 +Z 侧（南边）。
        // 所以 facing 直接取玩家朝向即可 —— 玩家朝南看，就得到 SOUTH，
        // 板贴在他面前的南侧。
        //
        // 注意：这里**不能**用 getHorizontalDirection().getOpposite()。
        // 那样站着朝南放，facing 会变成 NORTH，板贴到背面的北侧，
        // 视觉上就是「反了」。
        Direction facing = ctx.getHorizontalDirection();

        BlockPos rootPos = ctx.getClickedPos();
        Level level = ctx.getLevel();

        // 检查整块空间是否都空着
        for (int part = 0; part < partCount(); part++) {
            BlockPos p = worldPosOf(rootPos, part, facing);
            if (!level.getBlockState(p).canBeReplaced(ctx)) {
                return null;   // 放不下
            }
        }
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(PART, 0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;

        Direction facing = state.getValue(FACING);
        // 补上其余占位格
        for (int part = 1; part < partCount(); part++) {
            BlockPos p = worldPosOf(pos, part, facing);
            level.setBlock(p, state.setValue(PART, part), Block.UPDATE_ALL);
        }
    }

    /** 第 part 格在世界的坐标 */
    private BlockPos worldPosOf(BlockPos root, int part, Direction facing) {
        BlockPos off = offsetOf(part);
        // 宽度方向 = facing 的顺时针右方
        Direction right = facing.getClockWise();
        return root.relative(right, off.getX()).above(off.getY());
    }

    // ------------------------------------------------------------------
    // 机械动力虚影辅助建造
    // ------------------------------------------------------------------

    /**
     * 手持同款车厢板时，让 Create 在该方向渲染幽灵方块并支持连续铺设。
     *
     * <p>注意：Create 的 {@code IPlacementHelper} / {@code PlacementOffset} 类在这里
     * <b>不能直接 import</b>——否则没装 Create 的玩家一加载这个方块类就会
     * {@code NoClassDefFoundError}。所以全部通过 {@code CreateCompat} 的反射桥接调用。
     *
     * <p>Create 的 {@code PlacementOffset.placeInWorld} 会替我们完成放置。
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        ItemInteractionResult viaCreate =
                com.af400.af400.compat.create.CreatePlacementBridge.tryPlace(this, stack, state, level, pos, player, hand, hit);
        if (viaCreate != null) {
            return viaCreate;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    // ------------------------------------------------------------------
    // 破坏：连带拆掉整块，只掉一个物品
    // ------------------------------------------------------------------

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            Direction facing = state.getValue(FACING);
            BlockPos root = rootOf(pos, state);
            for (int part = 0; part < partCount(); part++) {
                BlockPos p = worldPosOf(root, part, facing);
                if (p.equals(pos)) continue;
                BlockState other = level.getBlockState(p);
                if (other.is(this) && other.getValue(PART) == part) {
                    level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // 掉落表按 part=0 结算；但玩家往往破坏的是任意一格。
        // 所以这里不限制 part —— 由 playerWillDestroy 负责把整块统一清掉，
        // 保证「拆一次、掉一个」，不会出现拆掉整块却不掉东西。
        return super.getDrops(state, builder);
    }

    /**
     * 玩家破坏开始前：把整块的其余格子一并清掉。
     *
     * <p>实现要点：
     * <ul>
     *   <li>被破坏的如果是占位格，先把它「视作主体」交给原版流程，确保掉落结算正确</li>
     *   <li>其余格子用 {@code setBlock(AIR)} 清掉，它们的 {@code onRemove} 会因为
     *       {@code newState} 已是空气而跳过连带逻辑，不会递归</li>
     * </ul>
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            BlockPos root = rootOf(pos, state);
            // 清掉其余格
            for (int part = 0; part < partCount(); part++) {
                BlockPos p = worldPosOf(root, part, facing);
                if (p.equals(pos)) continue;
                BlockState other = level.getBlockState(p);
                if (other.is(this)) {
                    level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            // 让当前格以 part=0 的身份离开，掉落表才能命中
            if (state.getValue(PART) != 0) {
                BlockState asRoot = state.setValue(PART, 0);
                super.playerWillDestroy(level, pos, asRoot, player);
                return asRoot;
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @SuppressWarnings("deprecation")   // Block#getCloneItemStack 在 1.21.1 被整体标注过时，但仍是最直接的方式
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        // 只有主体格能正确取到物品；占位格也统一返回本体，
        // 保证创造模式中键 / 精准采集拿到的是同一个东西。
        return new ItemStack(this.asItem());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, DISPLAY_MOUNT, DISPLAY_COLUMN);
    }
}
