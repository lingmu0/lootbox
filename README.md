# 战利品箱（NeoForge 1.21.1）

所有箱子都是同一个 `lootbox:loot_box` 物品，箱子类型存放在 `minecraft:custom_data` 组件的 `loot_box_id` 字段中。示例：

```mcfunction
/lootbox give @s lootbox:common
/lootbox give @s lootbox:rare 3
```

## 数据包定义

每个箱子可选 `color` 字段，支持 `#RRGGBB` 或整数颜色值，用于给贴图染色。

在 `data/<命名空间>/loot_boxes/<id>.json` 中声明 `display_name_key`（翻译键；也兼容直接文本的 `display_name`）、`rolls` 和 `entries`。每个 entry 支持：

* `item`、`min`、`max`：物品 ID 和数量范围；
* `weight`：基础权重；
* `luck_weight`：最终权重会加上 `玩家幸运 × luck_weight`；
* `condition: {"type":"luck","min":2}`：内置幸运条件；
* `condition: {"type":"custom","id":"has_tag","display":"需要 VIP"}` 可由 KJS 注册并提供自定义显示；也可以使用 `display_key` 指定翻译键；其他 `condition.type` 也可直接使用已注册的条件 ID。

## KJS 接口

KubeJS 可以调用 `net.xuwu.lootbox.LootBoxApi`：

```js
const LootBoxApi = Java.loadClass('net.xuwu.lootbox.LootBoxApi')
LootBoxApi.registerCondition('has_tag', ctx => ctx.hasPlayer() && ctx.player().getTags().contains('vip'), ctx => '需要 VIP')
LootBoxApi.register('my_pack:vip', 'VIP 奖励箱', 1, LootBoxApi.entries(
    LootBoxApi.entry('minecraft:diamond', 1, 2, 10, 2, 'has_tag', '需要 VIP')
))
```

注册时也可以传入颜色值，例如 `LootBoxApi.register(..., 0x00E5FF)`。

复杂条件的 JEI 展示文本由 `entry(..., conditionText)` 提供；幸运条件会自动显示为 `幸运 ≥ N`。自动开箱器记录放置者 UUID，只有该玩家在线时才会以其幸运和 KJS 条件进行判断；顶部输入、四周/底部输出均支持漏斗。
