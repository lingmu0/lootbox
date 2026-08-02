# 战利品箱（NeoForge 1.21.1）

所有箱子都是同一个 `lootbox:loot_box` 物品，箱子类型存放在 `minecraft:custom_data` 组件的 `loot_box_id` 字段中。示例：

```mcfunction
/lootbox give @s lootbox:common
/lootbox give @s lootbox:rare 3
```

内置箱子等级顺序为：`common`、`unusual`、`rare`、`epic`、`legendary`、`endurance`。耐力奖励箱有 99.99% 概率再次获得耐力奖励箱；剩余 0.01% 中，80% 为 100 个钻石块，20% 为 100 个绿宝石块。

## 数据包定义

每个箱子可选 `color` 字段，支持 `#RRGGBB` 或整数颜色值，用于给贴图染色。

在 `data/<命名空间>/loot_boxes/<id>.json` 中声明 `display_name_key`（翻译键；也兼容直接文本的 `display_name`）、`rolls` 和 `entries`。每个 entry 支持：

* `item`、`min`、`max`：物品 ID 和数量范围；
* `tag`：物品标签 ID，例如 `{"tag":"minecraft:music_discs","weight":10}`；标签中的物品会等概率随机抽取，且该条目的总权重仍为 `10`；`item`、`tag`、`box` 三者选其一；
* `box`：将另一个战利品箱作为奖励，例如 `{"box":"lootbox:endurance","weight":99990}`；
* `weight`：基础权重；
* `luck_weight`：最终权重会加上 `玩家幸运 × luck_weight`；
* `condition: {"type":"luck","min":2}`：内置幸运条件；
* `condition: {"type":"custom","id":"has_tag","display":"需要 VIP"}` 可由 KJS 注册并提供自定义显示；也可以使用 `display_key` 指定翻译键；其他 `condition.type` 也可直接使用已注册的条件 ID。
* `jei_info_key`：加入jei_info，此处使用翻译键

内置奖励池还会尝试加入 Mekanism、Create、Iron's Spells 'n Spellbooks、Goety、Applied Energistics 2、Terra Entity、The Twilight Forest 和 The Aether 的代表性物品。它们只通过注册表 ID 查找；对应模组未安装或物品 ID 不存在时会自动跳过，不会产生硬依赖。

## 配置

* `config/lootbox-common.toml` 的 `hide_default_boxes`：隐藏内置奖励箱，同时从本模组创造物品栏和 JEI 中移除；默认 `false`。
* `config/lootbox-common.toml` 的 `mob_drops_enabled`：开关玩家击杀生物掉落奖励箱；默认 `true`。
* 同一文件中的 `enable_mekanism_rewards`、`enable_create_rewards`、`enable_irons_spellbooks_rewards`、`enable_goety_rewards`、`enable_ae2_rewards`、`enable_terra_entity_rewards`、`enable_twilight_forest_rewards` 和 `enable_aether_rewards`：分别开关对应模组的内置奖励池；默认均为 `true`。未安装对应模组时仍会自动跳过。
* 同一文件的 `mob_drop_chances`：分别配置六种箱子的掉落概率，默认值为普通 `0.05`、不寻常 `0.02`、稀有 `0.01`、史诗 `0.005`、传奇 `0.001`、耐力 `0.0001`。每次击杀最多掉落一个，按耐力、传奇、史诗、稀有、不寻常、普通的顺序判定。

## KJS 接口

KubeJS 可以调用 `net.xuwu.lootbox.LootBoxApi`：

```js
const LootBoxApi = Java.loadClass('net.xuwu.lootbox.LootBoxApi')
LootBoxApi.registerCondition('has_tag', ctx => ctx.hasPlayer() && ctx.player().getTags().contains('vip'), ctx => '需要 VIP')
LootBoxApi.register('my_pack:vip', 'VIP 奖励箱', 1, LootBoxApi.entries(
    LootBoxApi.entry('minecraft:diamond', 1, 2, 10, 2, 'has_tag', '需要 VIP')
))
```

数据包和 KJS 的功能保持一一对应：

| 数据包字段 | KJS 方法 | 说明 |
| --- | --- | --- |
| `display_name` | `register` | 直接文本箱名 |
| `display_name_key` | `registerTranslated` | 翻译键箱名 |
| `color`、`rolls` | `register(..., color)` | 箱子颜色和抽取次数 |
| `item` | `entry` | 普通物品奖励 |
| `tag` | `entryTag` | 标签内物品等概率随机，标签延迟到使用时解析 |
| `box` | `entryBox` | 将另一个箱子作为奖励 |
| `jei_info_key` | `register(..., color, jeiInfoKey)` | JEI Info 翻译键 |
| `condition.display` | `entry(..., conditionText)` | 条件的直接显示文本 |
| `condition.display_key` | `entryWithConditionKey`、`entryTagWithConditionKey`、`entryBoxWithConditionKey` | 条件的翻译键 |
| `weight`、`luck_weight`、`min`、`max` | 所有 `entry*` 方法 | 权重、幸运权重和数量范围 |

标签奖励示例（标签内容会在标签加载或刷新后解析）：

```js
LootBoxApi.register('my_pack:tag_rewards', '标签奖励箱', 1, LootBoxApi.entries(
    LootBoxApi.entryTag('minecraft:music_discs', 1, 1, 10, 0, '', '')
))
```

箱子奖励和翻译键示例：

```js
LootBoxApi.registerTranslated('my_pack:translated', 'lootbox.example.translated', 1, LootBoxApi.entries(
    LootBoxApi.entryBox('lootbox:rare', 1, 1, 2, 0, '', ''),
    LootBoxApi.entryWithConditionKey('minecraft:diamond', 1, 2, 1, 0, 'has_tag', 'condition.example.vip')
), 0x00E5FF)
```

也可以为自定义箱子写入 JEI Info 翻译键：

```js
LootBoxApi.register('my_pack:info', '带说明的奖励箱', 1, LootBoxApi.entries(
    LootBoxApi.entry('minecraft:diamond', 1, 1, 1, 0, '', '')
), 0x00E5FF, 'lootbox.example.info')
```

数据包则在箱子 JSON 根对象中加入 `"jei_info_key": "lootbox.example.info"`。六个内置箱子会自动显示玩家击杀生物的获取方式和对应配置掉落概率。

注册时也可以传入颜色值，例如 `LootBoxApi.register(..., 0x00E5FF)`。

如果 KJS 覆盖注册 `lootbox:common`、`lootbox:rare` 等内置箱，跨模组可选奖励池也会和数据包定义一样自动追加；自定义命名空间的箱子不会自动追加。

复杂条件的 JEI 展示文本由 `entry(..., conditionText)` 或对应的 `*WithConditionKey` 方法提供；幸运条件会自动显示为 `幸运 ≥ N`。自动开箱器记录放置者 UUID，只有该玩家在线时才会以其幸运和 KJS 条件进行判断；顶部输入、四周/底部输出均支持漏斗。

后续新增奖励字段、条件或奖励类型时，会同时加入数据包解析器和 `LootBoxApi`，并同步更新此表和示例。
