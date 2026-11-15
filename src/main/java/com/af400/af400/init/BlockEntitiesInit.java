package com.af400.af400.init;

import com.af400.af400.AF400;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块实体注册表。
 *
 * <p>用法示例：
 * <pre>{@code
 * public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<XxxBlockEntity>> XXX =
 *         BLOCK_ENTITIES.register("xxx", () -> BlockEntityType.Builder
 *                 .of(XxxBlockEntity::new, BlocksInit.XXX.get())
 *                 .build(null));
 * }</pre>
 */
public class BlockEntitiesInit {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AF400.MOD_ID);

    static {
        AF400.LOGGER.debug("BlockEntitiesInit loaded");
    }
}
