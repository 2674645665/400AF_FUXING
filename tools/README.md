# 构建与版本说明

## 一键构建

```powershell
.\tools\build.ps1 -Note "改了什么东西"
```

流程：**判定版本类型 → 升版本 → 构建 → 存档到 `dist\<版本>\` → 更新清单**

### 常用参数

| 参数 | 说明 |
|---|---|
| `-Kind auto` | 默认。自动判定加方块 / 普通更新 |
| `-Kind block` | 强制按「加方块」处理 |
| `-Kind update` | 强制按「普通更新」处理 |
| `-Note "..."` | 版本说明，写进 CHANGELOG 和版本历史 |
| `-NoBump` | 不升版本，用当前版本号重新构建（修构建问题时用） |
| `-NoBuild` | 只升版本不构建 |

---

## 版本规则

| 改动 | 结果 | 例子 |
|---|---|---|
| 加方块 | 第三位 +1，**后缀重置** | `0.0.1` → `0.0.2`<br>`0.0.1_A3` → `0.0.2` |
| 其他更新 | 后缀递增 | `0.0.2` → `0.0.2_A0` → `0.0.2_A1` → … → `0.0.2_A9` → `0.0.2_B0` → `0.0.2_B1` |

- `A9` 之后自动进位到 `B0`，`B9` 之后是 `C0`，依此类推直到 `Z9`
- 版本号存在 [gradle.properties](../gradle.properties) 的 `mod_version`

### 自动判定依据

`-Kind auto` 时按下面的顺序判定：

1. **Git 仓库且能取到历史**：看 `src/main/resources/assets/af400/blockstates/` 和 `src/main/java/com/af400/af400/init/` 里有没有**新增**文件 → 有就是加方块
2. **非 Git 环境**：拿当前方块清单和 `dist/.baseline/blocks.txt` 快照比对 → 有新增方块就是加方块

方块清单来源：`blockstates/**.json` 的文件名 + Java 里 `BLOCKS.register...("名字")` 的字面量。

---

## 版本存档

```
dist/
├── VERSIONS.md           总清单
├── 版本历史.json          结构化历史
├── .baseline/blocks.txt  方块快照（本地判断用，不入库）
├── 0.0.1/
│   ├── af400-1.21.1-neoforge-0.0.1.jar
│   ├── version.json      元信息 + SHA256
│   └── CHANGELOG.md
└── 0.0.2/
    └── ...
```

**所有版本永久保留**，不会覆盖。`dist/` 里的 jar 会随 git 一起推到 GitHub。

---

## 构建方式（重要）

脚本会自动探测 `maven.neoforged.net` 是否可达，然后二选一：

### 方式 A：Gradle（标准）

能访问 NeoForge 官方 maven 时使用。

```bash
./gradlew build
```

### 方式 B：手工构建（本机当前使用这个）

本机 `maven.neoforged.net` 无法访问，ModDevGradle 需要的 `neoform-runtime:1.0.5` 拉不到。
但 `~/.gradle/caches/neoformruntime/` 里已经有完整的 `compiledWithNeoForge` 产物
（Minecraft 1.21.1 + NeoForge 的反混淆编译结果），可以直接拿来当 classpath。

```powershell
.\tools\build-manual.ps1 -Version 0.0.2
```

强制指定用哪种：

```powershell
$env:AF400_FORCE_MANUAL=1   # 强制手工
$env:AF400_FORCE_GRADLE=1   # 强制 Gradle
```

> ⚠️ 手工构建不经过 Minecraft 的资源处理流程，只做编译 + 资源模板替换 + 打包。
> 一旦网络恢复，建议切回 Gradle 构建。

---

## 辅助脚本

| 脚本 | 用途 |
|---|---|
| `tools/version.ps1` | 版本号解析 / 递增 / 读写 gradle.properties |
| `tools/detect-change.ps1` | 判定加方块 vs 普通更新 |
| `tools/build.ps1` | 主流水线 |
| `tools/build-manual.ps1` | 绕过 Gradle 的手工构建 |
| `tools/resolve-deps.ps1` | 自动补齐缺失依赖到 `~/.m2`（网络受限时用） |

---

## 同步到游戏

构建完成后把 jar 拷到 mods 目录：

```
桌面\.minecraft\versions\Willow11TH_Aero_beta2(无地图)\mods\
```

---

## GitHub Actions 自动构建

推送到 `main` 后自动执行 [.github/workflows/build.yml](../.github/workflows/build.yml)：

1. 判定 + 升版本
2. Gradle 构建（CI 环境能正常访问 NeoForge maven）
3. 上传 Actions Artifacts
4. 创建 Release（附 jar）
5. 把 `dist\<版本>\` 自动提交回仓库

也可以手动触发，选择 `block` / `update` / `auto`。
