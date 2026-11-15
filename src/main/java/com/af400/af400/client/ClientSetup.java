package com.af400.af400.client;

import com.af400.af400.AF400;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 客户端初始化。<b>这是真正的客户端入口</b>（{@link com.af400.af400.AF400Client} 只是占位示例，可删）。
 *
 * <p>对照 HeXie 的 {@code client/HexieClient}：注册菜单界面、实体渲染器、按键、覆盖层……
 */
@Mod(value = AF400.MOD_ID, dist = Dist.CLIENT)
public class ClientSetup {

    public ClientSetup(IEventBus modBus, ModContainer container) {
        modBus.addListener(this::clientSetup);

        // ---- 以下是骨架钩子，内容就绪后逐个接上 ----
        modBus.addListener(this::registerScreens);      // 菜单 GUI
        modBus.addListener(this::registerRenderers);    // 方块实体 / 实体渲染器
        modBus.addListener(this::registerKeyMappings);  // 按键绑定
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            AF400.LOGGER.info("[400AF] client setup done.");
        });
    }

    private void registerScreens(final RegisterMenuScreensEvent event) {
        // event.register(ModMenus.XXX.get(), XxxScreen::new);
    }

    private void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        // event.registerBlockEntityRenderer(BlockEntitiesInit.XXX.get(), XxxRenderer::new);
    }

    private void registerKeyMappings(final RegisterKeyMappingsEvent event) {
        // event.register(KeyBindings.SOME_KEY);
    }
}
