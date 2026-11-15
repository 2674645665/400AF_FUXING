package com.af400.af400.init;

import com.af400.af400.AF400;
import net.neoforged.fml.ModList;

/**
 * 交互行为 / 第三方兼容的初始化入口。
 *
 * <p>400AF 依赖机械动力（Create）：
 * <ul>
 *   <li>{@code create} —— 虚影辅助建造（{@code IPlacementHelper}）、工具交互（扳手）、放置体系</li>
 *   <li>{@code ponder} —— 由 Create 内嵌，用于引导动画（后续实现）</li>
 * </ul>
 *
 * <p>所有 Create 相关的类都集中在 {@code com.af400.af400.compat.create} 包，
 * 且只在确认 Create 已加载后才会被加载，因此没装 Create 时不会出现
 * {@code NoClassDefFoundError}。
 *
 * <p>注册时机：在 mod 构造阶段立刻注册。{@code PlacementHelpers.HELPERS} 是
 * 纯静态双端共享列表，越早注册越好——客户端渲染幽灵方块时按 id 查表。
 */
public final class InteractionBehaviourInit {

    private InteractionBehaviourInit() {
    }

    /** 在 mod 构造阶段调用 */
    public static void init() {
        if (ModList.get().isLoaded("create")) {
            try {
                com.af400.af400.compat.create.CreateCompat.setup();
            } catch (Throwable t) {
                AF400.LOGGER.error("[400AF] 机械动力兼容初始化失败，虚影辅助建造将不可用", t);
            }
        } else {
            AF400.LOGGER.warn("[400AF] 未检测到机械动力（create），虚影辅助建造不可用");
        }
    }
}
