package com.af400.af400.compat.create;

import com.af400.af400.AF400;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;

/**
 * 机械动力兼容注册入口。
 *
 * <p>把 400AF 的放置辅助器注册进 Create 的 {@link PlacementHelpers} 列表，
 * 拿到 id 后由方块在 {@code useItemOn} 里查回来使用（与 Create 自身的
 * {@code GirderBlock} / {@code TableClothBlock} 做法一致）。
 *
 * <p>本类只在安装 Create 时才会被加载，见 {@link #setup()}。
 */
public final class CreateCompat {

    /** 车厢板放置辅助器的注册 id；-1 表示未注册 */
    private static int panelHelperId = -1;

    private CreateCompat() {
    }

    /**
     * 注册所有 400AF 的放置辅助器。
     * 由 {@link com.af400.af400.init.InteractionBehaviourInit} 在 Create 存在时调用一次。
     */
    public static void setup() {
        if (panelHelperId >= 0) {
            return;
        }
        panelHelperId = PlacementHelpers.register(new PanelPlacementHelper());
        AF400.LOGGER.info("[400AF] 已注册车厢板虚影辅助建造（Create placement helper id={}）", panelHelperId);
    }

    /** 取回车厢板辅助器；未注册或未装 Create 时返回 null */
    public static IPlacementHelper panelHelper() {
        if (panelHelperId < 0) {
            return null;
        }
        return PlacementHelpers.get(panelHelperId);
    }

    public static boolean isRegistered() {
        return panelHelperId >= 0;
    }
}
