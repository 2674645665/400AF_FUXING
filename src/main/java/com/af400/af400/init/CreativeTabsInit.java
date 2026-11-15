package com.af400.af400.init;

import com.af400.af400.AF400;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 创造模式标签页。
 */
public class CreativeTabsInit {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AF400.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.af400.main"))
                    .icon(() -> new ItemStack(ItemsInit.EXAMPLE_PART.get()))
                    .displayItems((params, output) -> {
                        // output.accept(ItemsInit.XXX.get());
                    })
                    .build()
    );

    static {
        AF400.LOGGER.debug("CreativeTabsInit loaded");
    }
}
