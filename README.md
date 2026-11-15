# 400AF_FUXING

CR400AF（复兴号）Minecraft 模组 — NeoForge 1.21.1 / Create 6.0.10

---

## 目标平台

| 项目 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.233 |
| Create（机械动力） | 6.0.10 |
| SimuRail | 0.1.0（可选） |
| Java | 21 |

---

## 构建

需要 **JDK 21**。

```bash
./gradlew build          # 编译打包 → build/libs/
./gradlew runClient      # 启动客户端
./gradlew runData        # 生成数据到 src/generated/resources
```

### 一键构建 + 存档（推荐）

```powershell
.\tools\build.ps1 -Note "加了车体侧板"
```

这个脚本会：判定版本类型 → 升版本 → 构建 → 产物存到 `dist\<版本>\` → 更新 `dist\VERSIONS.md`。

---

## 版本规则

版本号形如 `0.0.2` 或 `0.0.2_A1`。

| 改动类型 | 版本变化 | 例子 |
|---|---|---|
| **加方块** | 第三位 +1，后缀重置 | `0.0.1` → `0.0.2`；`0.0.1_A3` → `0.0.2` |
| **其他更新** | 后缀递增 | `0.0.2` → `0.0.2_A0` → `0.0.2_A1` → … → `0.0.2_A9` → `0.0.2_B0` |

- A 段用尽（`A9` 之后）自动进位到 `B0`
- 版本号记在 [gradle.properties](gradle.properties) 的 `mod_version`

自动判定依据：`src/main/resources/assets/af400/blockstates/` 里新增了 `.json` 就视为**加方块**，否则是**普通更新**。也可以用 `-Kind block` / `-Kind update` 手动指定。

---

## 版本存档

所有构建版本都保留在 [dist/](dist/) 下，每版一个文件夹：

```
dist/
├── VERSIONS.md          # 总清单
├── 版本历史.json
├── 0.0.1/
│   ├── 400AF-1.21.1-neoforge-0.0.1.jar
│   ├── version.json     # 版本元信息 + SHA256
│   └── CHANGELOG.md
└── 0.0.2/
    └── ...
```

---

## 自动构建（GitHub Actions）

推送到 `main` 分支后自动跑 [.github/workflows/build.yml](.github/workflows/build.yml)：

1. 自动判定并升版本
2. 构建
3. 产物上传到 Actions Artifacts
4. 创建 Release（附 jar）
5. 把 `dist\<版本>\` 提交回仓库

也可以在 Actions 页面手动触发，选择 `block` / `update` / `auto`。

---

## 结构

工程结构参照 HeXie mod（`Hexie-1.21.1-NeoForge-0.9.3`）的组织方式。
详见 [STRUCTURE.md](STRUCTURE.md)。

---

## 许可

MIT — 见 [LICENSE](LICENSE)
