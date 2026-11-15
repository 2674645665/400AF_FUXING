package com.af400.af400.init;

import com.af400.af400.AF400;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册表。对照 HeXie 的 {@code init/item/*} 与 {@code item/*}。
 *
 * <p>分区：方块物品 / 工具与零件 / 票券 / 食物 / 可编辑面板 / 信号机。
 */
public class ItemsInit {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AF400.MOD_ID);

    // ---- 车厢板 ----

    /** 400AF 正常小窗车厢板 */
    public static final DeferredItem<BlockItem> AF400_PANEL_SMALL_WINDOW = ITEMS.registerSimpleBlockItem(
            "af400_panel_small_window", BlocksInit.AF400_PANEL_SMALL_WINDOW);

    /** 400AF 宽客车厢板 */
    public static final DeferredItem<BlockItem> AF400_PANEL_WIDE_WINDOW = ITEMS.registerSimpleBlockItem(
            "af400_panel_wide_window", BlocksInit.AF400_PANEL_WIDE_WINDOW);

    /** 400AF 无窗车厢板 */
    public static final DeferredItem<BlockItem> AF400_PANEL_NO_WINDOW = ITEMS.registerSimpleBlockItem(
            "af400_panel_no_window", BlocksInit.AF400_PANEL_NO_WINDOW);

    // ---- 后续补充 ----
    // 注意：文本贴纸直接用 HeXie 的 hexie:panel_text，本模组不再自带。
    // 原因：我们的板挂的是 CRH2PartEntity，HeXie 的物品天然通用。

    // ---- 后续补充 ----
    // 票券   (item/tickets)
    // 食物   (item/foods)
    // 面板   (item/editable_panels)
    // 信号   (item/semaphore)
    // 轨道   (item/track)
    static {
        AF400.LOGGER.debug("ItemsInit loaded");
    }
}
