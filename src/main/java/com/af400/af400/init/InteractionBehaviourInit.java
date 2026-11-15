package com.af400.af400.init;

import com.af400.af400.AF400;

/**
 * 交互行为（Interaction Behaviour）注册入口 —— <b>占位，待填</b>。
 *
 * <p>对照 HeXie 的 {@code init/InteractionBehaviourInit} + {@code interaction/compatible}。
 * HeXie 把自定义方块挂到 Create 的交互体系（座椅 / 车门 / 扳手 / 控制器……）具体靠哪个注册表，
 * 取决于你用的 Create 6.0.10 版本里 InteractionBehaviours 的实际 API。
 *
 * <p>TODO（内容阶段）：先确认 Create 6.0.10 里 InteractionBehaviour 的序列化器注册表路径，
 * 再在这里建 {@code DeferredRegister} 并在 AF400 主类里 {@code register(modBus)}。
 * 现在不注册任何东西，避免编译期引用不存在的类。
 */
public final class InteractionBehaviourInit {

    private InteractionBehaviourInit() {
    }

    /* 使用前的自然调用点（AF400 主类里已注释）：
     *
     * public static final DeferredRegister<InteractionBehaviourSerializer<?>> BEHAVIOURS =
     *         DeferredRegister.create(<Create 的交互行为序列化器注册表>, AF400.MOD_ID);
     */

    static {
        AF400.LOGGER.debug("InteractionBehaviourInit placeholder loaded");
    }
}
