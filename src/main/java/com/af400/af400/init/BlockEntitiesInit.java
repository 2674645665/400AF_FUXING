package com.af400.af400.init;

import com.af400.af400.AF400;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块实体注册表。
 *
 * <p><b>当前为空</b>：三块车厢板直接使用 HeXie 的
 * {@code CRH2PartEntity}（它同时实现 {@code VehicleMarkings} / {@code PassengerDisplayHost} /
 * {@code MenuProvider}，贴纸、水牌、编辑界面全部复用 HeXie）。
 * 为了让 MC 接受「我们的板 + HeXie 的实体类型」这个组合，
 * 由 {@code MixinBlockEntityTypeAddBlocks} 把方块补进 {@code crh2a_part} 的支持列表。
 *
 * <p>以后若需要自有实体（例如不依赖 HeXie 的场景），在这里注册并改
 * {@code CarriagePanelBlock#newBlockEntity} 即可。
 */
public class BlockEntitiesInit {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AF400.MOD_ID);

    static {
        AF400.LOGGER.debug("BlockEntitiesInit loaded");
    }
}
