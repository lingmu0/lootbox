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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/** 战利品箱主类。定义本身来自数据包，物品只有一个注册表 ID。 */
@Mod(LootBoxMod.MODID)
public class LootBoxMod {
    public static final String MODID = "lootbox";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<LootBoxItem> LOOT_BOX = ITEMS.register("loot_box",
            () -> new LootBoxItem(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<LootBoxOpenerBlock> LOOT_BOX_OPENER = BLOCKS.register("loot_box_opener",
            () -> new LootBoxOpenerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F)));
    public static final RegistryObject<BlockItem> LOOT_BOX_OPENER_ITEM = ITEMS.register("loot_box_opener",
            () -> new BlockItem(LOOT_BOX_OPENER.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<LootBoxOpenerBlockEntity>> LOOT_BOX_OPENER_ENTITY =
            BLOCK_ENTITIES.register("loot_box_opener", () -> BlockEntityType.Builder.of(
                    LootBoxOpenerBlockEntity::new, LOOT_BOX_OPENER.get()).build(null));

    public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_MODE_TABS.register(
            "loot_boxes", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.lootbox.loot_boxes"))
                    .withTabsBefore(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                    .icon(() -> LootBoxItem.createStack("lootbox:common"))
                    .displayItems((parameters, output) -> {
                        for (LootBoxDefinition definition : LootBoxManager.creativeDefinitions()) {
                            output.accept(LootBoxItem.createStack(definition.id().toString()));
                        }
                        output.accept(LOOT_BOX_OPENER_ITEM.get());
                    }).build());

    public LootBoxMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, LootBoxConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, LootBoxConfig.SPEC);
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addCreative);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LootBoxApi.registerBuiltinConditions();
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(LOOT_BOX_OPENER_ITEM.get());
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
    public void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        LootBoxMobDrops.handle(event);
    }
}
