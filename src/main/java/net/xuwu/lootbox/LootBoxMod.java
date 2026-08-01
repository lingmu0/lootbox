package net.xuwu.lootbox;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/** 战利品箱主类。定义本身来自数据包，物品只有一个注册表 ID。 */
@Mod(LootBoxMod.MODID)
public class LootBoxMod {
    public static final String MODID = "lootbox";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String[] DEFAULT_BOXES = {
            "lootbox:common", "lootbox:unusual", "lootbox:rare",
            "lootbox:epic", "lootbox:legendary", "lootbox:endurance"
    };

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredItem<LootBoxItem> LOOT_BOX = ITEMS.register("loot_box",
            () -> new LootBoxItem(new Item.Properties().stacksTo(64)));

    public static final DeferredBlock<LootBoxOpenerBlock> LOOT_BOX_OPENER = BLOCKS.register("loot_box_opener",
            () -> new LootBoxOpenerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F)));
    public static final DeferredItem<BlockItem> LOOT_BOX_OPENER_ITEM = ITEMS.registerSimpleBlockItem("loot_box_opener", LOOT_BOX_OPENER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LootBoxOpenerBlockEntity>> LOOT_BOX_OPENER_ENTITY =
            BLOCK_ENTITIES.register("loot_box_opener", () -> BlockEntityType.Builder.of(
                    LootBoxOpenerBlockEntity::new, LOOT_BOX_OPENER.get()).build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register(
            "loot_boxes", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.lootbox.loot_boxes"))
                    .withTabsBefore(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                    .icon(() -> LootBoxItem.createStack("lootbox:common"))
                    .displayItems((parameters, output) -> {
                        if (!LootBoxConfig.HIDE_DEFAULT_BOXES.get()) {
                            for (String boxId : DEFAULT_BOXES) output.accept(LootBoxItem.createStack(boxId));
                        }
                        output.accept(LOOT_BOX_OPENER_ITEM);
                    }).build());

    public LootBoxMod(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, LootBoxConfig.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, LootBoxConfig.SPEC);
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addCreative);
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LootBoxApi.registerBuiltinConditions();
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(LOOT_BOX_OPENER_ITEM);
        }
    }

    @SubscribeEvent
    public void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new LootBoxManager());
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        LootBoxCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onLivingDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        LootBoxMobDrops.handle(event);
    }
}
