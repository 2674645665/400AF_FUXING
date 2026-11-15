# 400AF 工程结构说明

结构参照 `Hexie-1.21.1-NeoForge-0.9.3`（jar 反解）的组织方式，
包名/命名空间做了替换：`willow.train.hexie` → `com.af400.af400`，`hexie` → `af400`。

---

## 一、工程根

| 路径 | 说明 |
|---|---|
| `build.gradle` | ModDevGradle 构建脚本，含 client/server/data 三个 run 配置 |
| `settings.gradle` | 插件仓库（NeoForged / CreateMod） |
| `gradle.properties` | **所有版本号集中在这里**，改版本只改这个文件 |
| `gradlew` / `gradlew.bat` / `gradle/wrapper/` | Gradle 包装器（首次运行会下载 gradle 发行版） |
| `src/main/java/` | Java 源码 |
| `src/main/resources/` | 资源与数据 |
| `src/generated/resources/` | `runData` 的产物（暂未生成） |
| `run/client`、`run/server`、`run/data` | 游戏运行目录 |

---

## 二、Java 包结构

根包 `com.af400.af400`，入口 `AF400.java`（对照 HeXie 的 `Main`）。

### 入口与初始化

| 类 | 对照 HeXie | 说明 |
|---|---|---|
| `AF400` | `Main` | → `@Mod` 主入口，注册所有 DeferredRegister、配置、生命周期 |
| `client.ClientSetup` | `client/HexieClient` | → 客户端专属入口（渲染器 / GUI / 按键注册钩子） |
| `init.BlocksInit` | `init/BlockInit*` | 方块注册表（含一个示例方块 `example_part`） |
| `init.ItemsInit` | `init/ItemInit*` | 物品注册表（含示例方块物品） |
| `init.BlockEntitiesInit` | `init/BlockEntitiesInit` | 方块实体 |
| `init.EntityInit` | `init/EntityInit` | 实体 |
| `init.CreativeTabsInit` | `init/CreativeTabsInit` | 创造标签 `af400:main` |
| `init.SoundsInit` | `init/SoundsInit` | 音效 |
| `init.EffectInit` | `init/EffectInit` | 状态效果 |
| `init.RecipeInit` | `init/recipe/*` | 配方类型与序列化器 |
| `init.InteractionBehaviourInit` | `init/InteractionBehaviourInit` | **占位**，待确认 Create API 后启用 |
| `config.AF400Config` | `config/` | → 生成 `config/af400-common.toml` |

### 业务包

```
api/            cab  carriage  display  interaction  traincontrol
blockentity/
blocks/         base  bogeys  carriage  coupler  emu  locos  parts
                seats  semaphore  signs  slidingdoor  structure
                ticketvendor  track  turnstile
                emu/{head, af400}   locos/{cr400af}
                semaphore/through   structure/weathering   track/standard
client/         cab  carriage  events  interaction  model  render  screens  sign  util
                carriage/browser
                render/{blockentity, bogey, carriage, display, entity, track}
                screens/widget   util/gui
compat/         create  iris  sable
                create/client  create/railx{,/client}  iris/client
config/
creative/
datafix/
effect/
entity/
event/          client/
init/           block  bogeys  client  item  recipe  semaphore  track
interaction/    compatible/
item/           editable_panels  foods  semaphore  tickets  track
menu/
mixins/mixin/   client/
network/        c2s{,/signs}   s2c/
util/           enums  station
vehicle/        assembly  bogey  control  kinematics  player  runtime  track
```

每个空目录里都有 `README.md` 说明用途。

---

## 三、资源结构

命名空间统一为 `af400`。

### `assets/`
```
assets/af400/
├── af400 ...（无此文件）
├── blockstates/      ← 方块状态
├── carriage_ui/      ← 车厢 UI
├── endface/          ← 车端面
├── lang/             ← en_us.json, zh_cn.json ✔
├── models/{block,item,component}/
├── schematic/        ← 样板附属资源
├── shaders/
├── sounds/           ← .ogg
├── textures/{block,component,entity,gui,item,laquered_board,logo,misc,mob_effect,recipes}/
└── sounds.json ✔
assets/minecraft/{atlases, textures/entity/signs}/   ← 对原版的覆盖
```

### `data/`
```
data/af400/
├── advancement/{, recipes/}
├── af400_displays/   ← 显示牌定义（HeXie 对应 crh2a_displays）
├── af400_parts/      ← 部件定义（HeXie 对应 crh2a_parts）
├── loot_table/blocks/  ✔ 有一个示例
├── recipe/
├── vehicle/
└── （根目录预留给零件/车体几何 json，如 HeXie 的 crh2a_*_geometry.json）

data/c/tags/block/                 ← c 通用标签
data/create/tags/{block,item}/     ← 向 Create 追加标签
data/minecraft/tags/{block{,/mineable},item}/
data/neoforge/data_maps/block/     ← oxidizables / waxables
```

### `META-INF/`
- `neoforge.mods.toml` ✔ — 已写好，依赖 NeoForge / MC / Create（必需）、SimuRail（可选）
- `af400.mixins.json` ✔ — 空 mixins 列表，按需往里加类名
- `pack.mcmeta` ✔

### 根
- `af400.png` ✔ — 模组图标（占位纯色，128×128）

---

## 四、当前可运行状态

- ✅ 目录骨架完整
- ✅ 主类 + 注册表全部就位（`example_part` 方块完整走通了注册→模型→掉落表链路）
- ✅ lang / sounds / mods.toml / mixins 配置齐全
- ✅ 配置类就位
- ⚠️ 未执行 `gradlew build`（本机无 Gradle 发行版缓存），首次构建需联网下载依赖
- ⚠️ `InteractionBehaviourInit` 为占位，未在注册表中实际注册

---

## 五、下一步

1. 跑一次 `gradlew build` 确认工具链通
2. 从 `blocks/parts` 开始填具体车体方块（模型→贴图→blockstate→掉落表→标签）
3. 建立车门 / 座椅交互，接入 Create 的 Interaction Behaviour
4. 车厢实体与编组装配（`vehicle/`）
5. 客户端渲染器（`client/render/`）
