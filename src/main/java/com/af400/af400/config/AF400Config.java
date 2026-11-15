package com.af400.af400.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 通用配置。对照 HeXie 的 {@code config/} 与服务器侧的 {@code hexie-server.toml}。
 */
public class AF400Config {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** 调试开关：输出列车运行时详细日志 */
    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("输出 400AF 运行时详细日志")
            .define("debugLogging", false);

    /** 车厢乘客上限 */
    public static final ModConfigSpec.IntValue MAX_PASSENGERS = BUILDER
            .comment("单节车厢最大乘客数")
            .defineInRange("maxPassengers", 32, 1, 256);

    /** 允许在其他 mod 的列车上挂 400AF 车厢 */
    public static final ModConfigSpec.BooleanValue ALLOW_CROSS_COMPAT = BUILDER
            .comment("允许与其它铁路 mod 的车厢混编")
            .define("allowCrossCompat", true);

    static {
        BUILDER.push("vehicle");
        SPEC = BUILDER.build();
    }

    private AF400Config() {
    }
}
