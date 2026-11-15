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

    // ---- 示例：对应 BlocksInit.EXAMPLE_PART 的方块物品 ----
    public static final DeferredItem<BlockItem> EXAMPLE_PART = ITEMS.registerSimpleBlockItem(
            "example_part", BlocksInit.EXAMPLE_PART
    );

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
