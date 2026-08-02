# 战利品箱（Forge 1.20.1）

这是一个以“定义驱动”为核心的战利品箱模组：物品注册表中只有一个 `lootbox:loot_box`，具体箱子类型由物品 NBT 中的 `loot_box_id` 标识。战利品箱可以来自内置内容、数据包或 KubeJS；服务器会在玩家加入和 `/reload` 后把最终定义同步给客户端，因此联机时 JEI、创造物品栏和提示框使用的内容与服务器一致。

## 快速开始

```mcfunction
/lootbox give @s lootbox:common
/lootbox give @s lootbox:rare 3
```

内置等级从低到高为：

`common` → `unusual` → `rare` → `epic` → `legendary` → `endurance`

耐力奖励箱只有 0.01% 的机会进入奖励判定，其中 80% 给出 100 个钻石块，20% 给出 100 个绿宝石块；其余 99.99% 会再次给出耐力奖励箱。

内置箱子会尝试加入 Mekanism、Create、Iron's Spells 'n Spellbooks、Goety、Applied Energistics 2、Terra Entity、The Twilight Forest 和 The Aether 的代表性物品。对应模组未安装时会自动跳过，不会产生硬依赖。

## 数据包

文件位置：`data/<命名空间>/loot_boxes/<id>.json`

下面是一个完整示例，建议先复制它，再按需删减字段：

```json
{
  "display_name_key": "example.lootbox.name",
  "color": "#00E5FF",
  "rolls": 2,
  "jei_info_key": "example.lootbox.jei_info",
  "entries": [
    {
      "item": "minecraft:diamond",
      "min": 1,
      "max": 3,
      "weight": 10,
      "luck_weight": 2,
      "condition": { "type": "luck", "min": 2 }
    },
    {
      "tag": "minecraft:music_discs",
      "min": 1,
      "max": 1,
      "weight": 5
    },
    {
      "box": "lootbox:rare",
      "min": 1,
      "max": 1,
      "weight": 1
    }
  ]
}
```

根对象字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `display_name_key` | 字符串 | 箱子名称翻译键，推荐使用；也可以用 `display_name` 写直接文本。 |
| `color` | `#RRGGBB`、`0xRRGGBB` 或整数 | 箱子贴图颜色，默认白色。 |
| `rolls` | 整数 | 每次开箱抽取次数，默认 1。 |
| `jei_info_key` | 字符串 | JEI Info 翻译键；也可以用 `jei_info` 写直接文本。 |
| `entries` | 数组 | 奖励条目。每项必须在 `item`、`tag`、`box` 中选择一个。 |

奖励条目还支持：

- `item`：固定物品 ID。
- `tag`：物品标签 ID。标签内的物品等概率随机选择，但该条目的总权重不变；标签会在标签刷新后重新解析。
- `box`：另一个战利品箱的 ID。
- `min`、`max`：数量范围。
- `weight`：基础权重。
- `luck_weight`：最终权重为 `weight + 玩家幸运 × luck_weight`。
- `condition: {"type":"luck","min":2}`：要求玩家幸运值至少为 2。
- `condition.display_key` 或 `condition.display`：为已注册的自定义条件提供 JEI/提示框显示文本。

## KubeJS

箱子定义应放在 `kubejs/server_scripts/lootbox.js`，这样 `/reload` 后会重新执行；`startup_scripts` 适合只在完整重启时注册的内容。建议每次脚本重载前清理上一批脚本定义：

```js
const LootBoxApi = Java.loadClass('net.xuwu.lootbox.LootBoxApi')

LootBoxApi.clearScriptedDefinitions()
LootBoxApi.registerCondition(
  'has_vip_tag',
  ctx => ctx.hasPlayer() && ctx.player().getTags().contains('vip'),
  ctx => '需要 VIP'
)

LootBoxApi.registerTranslated(
  'example:vip',
  'example.lootbox.vip',
  1,
  LootBoxApi.entries(
    LootBoxApi.entry('minecraft:diamond', 1, 2, 10, 2, 'has_vip_tag', ''),
    LootBoxApi.entryTag('minecraft:music_discs', 1, 1, 5, 0, '', ''),
    LootBoxApi.entryBox('lootbox:rare', 1, 1, 1, 0, '', '')
  ),
  0x00E5FF,
  'example.lootbox.vip_jei_info'
)
```

数据包字段与 KJS 方法的对应关系：

| 数据包 | KJS | 说明 |
| --- | --- | --- |
| `display_name` | `register` | 直接文本名称。 |
| `display_name_key` | `registerTranslated` | 翻译键名称。 |
| `item` | `entry` | 固定物品奖励。 |
| `tag` | `entryTag` | 标签内物品等概率随机。 |
| `box` | `entryBox` | 另一个箱子作为奖励。 |
| `jei_info_key` | `register(..., color, jeiInfoKey)` | JEI Info 翻译键。 |
| `condition.display_key` | `entryWithConditionKey`、`entryTagWithConditionKey`、`entryBoxWithConditionKey` | 条件显示翻译键。 |
| `min`、`max`、`weight`、`luck_weight` | 所有 `entry*` 方法 | 数量、权重和幸运权重。 |

标签、箱子和翻译键也可以单独组合：

```js
LootBoxApi.registerTranslated(
  'example:translated',
  'example.lootbox.translated',
  1,
  LootBoxApi.entries(
    LootBoxApi.entryTagWithConditionKey(
      'minecraft:music_discs', 1, 1, 10, 0, 'has_vip_tag', 'example.condition.vip'
    ),
    LootBoxApi.entryBox('lootbox:endurance', 1, 1, 1, 0, '', '')
  ),
  0x7C4DFF,
  'example.lootbox.translated_jei_info'
)
```

## 配置

所有配置都位于 `config/lootbox-common.toml`，并且由服务器配置决定实际玩法：

- `hide_default_boxes`：隐藏内置箱子，同时从创造物品栏和 JEI 中移除；默认 `false`。
- `mob_drops_enabled`：是否允许击杀生物掉落内置箱子；默认 `true`。
- `enable_mekanism_rewards`、`enable_create_rewards`、`enable_irons_spellbooks_rewards`、`enable_goety_rewards`、`enable_ae2_rewards`、`enable_terra_entity_rewards`、`enable_twilight_forest_rewards`、`enable_aether_rewards`：分别控制可选模组联动奖励；默认均为 `true`。
- `mob_drop_chances`：配置六种内置箱子的掉落概率。每次击杀最多掉落一个，判定顺序为耐力、传奇、史诗、稀有、不寻常、普通。

客户端收到服务器快照后，会用服务器的箱子列表和隐藏设置刷新 JEI 与创造栏；客户端本地配置只在尚未连接服务器时作为界面回退值使用。

## JEI

安装 JEI 后，每个可见战利品箱会显示一个“输入箱子 → 可能奖励”的条目：

- 输入槽和输出槽都使用 JEI 的标准 slot 背景。
- 标签奖励会展开为标签中的各个物品，并按等概率拆分权重。
- 悬停奖励会显示数量、权重、幸运权重、当前幸运值下的最终概率和条件。
- 不满足幸运条件的奖励与 tooltip 保持一致，显示 `0.00%`。
- `jei_info_key` 或内置箱子的获取方式/掉落概率会显示在 JEI 条目下方，英文等长文本会自动换行。

## 联机与重载

1. 数据包放入世界的 `datapacks` 目录，KJS 定义放入服务端 `kubejs/server_scripts`。
2. 执行 `/reload`。
3. 服务器重新计算数据包与 KJS 定义后，会把最终快照发送给所有在线玩家。
4. 玩家端自动刷新 JEI、创造物品栏、箱子名称、颜色和奖励展示；不需要让每个客户端重复安装或执行同一份脚本。

如果看不到新箱子，请先确认服务器和客户端的模组版本一致，再检查 `hide_default_boxes`、脚本日志以及 `/reload` 后的服务器日志。未知或被移除的定义仍会通过物品快照安全显示，不会让已有箱子导致崩溃。
