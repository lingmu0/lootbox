package net.xuwu.lootbox;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/** 可选 JEI 集成：以“输入一个箱子，输出所有可能奖励”的方式展示。 */
@JeiPlugin
public final class LootBoxJeiPlugin implements IModPlugin {
    public static final RecipeType<LootBoxJeiRecipe> RECIPE_TYPE =
            RecipeType.create(LootBoxMod.MODID, "loot_box", LootBoxJeiRecipe.class);
    private static volatile IJeiRuntime RUNTIME;
    private static volatile List<LootBoxJeiRecipe> REGISTERED_RECIPES = List.of();

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(LootBoxMod.MODID, "jei_plugin");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(LootBoxMod.LOOT_BOX.get(),
                (stack, context) -> LootBoxItem.getDefinitionId(stack).toString());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<LootBoxJeiRecipe> recipes = buildRecipes();
        REGISTERED_RECIPES = recipes;
        if (!recipes.isEmpty()) registration.addRecipes(RECIPE_TYPE, recipes);

        for (LootBoxDefinition definition : LootBoxManager.creativeDefinitions()) {
            List<Component> info = LootBoxManager.jeiInfo(definition);
            if (!info.isEmpty()) {
                registration.addIngredientInfo(LootBoxItem.createStack(definition.id().toString()),
                        VanillaTypes.ITEM_STACK, info.toArray(Component[]::new));
            }
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        RUNTIME = runtime;
    }

    /** Refreshes the runtime recipe list after a server datapack/KJS sync. */
    public static void refreshRuntimeRecipes() {
        IJeiRuntime runtime = RUNTIME;
        if (runtime == null) return;
        List<LootBoxJeiRecipe> oldRecipes = REGISTERED_RECIPES;
        if (!oldRecipes.isEmpty()) runtime.getRecipeManager().hideRecipes(RECIPE_TYPE, oldRecipes);
        List<LootBoxJeiRecipe> newRecipes = buildRecipes();
        if (!newRecipes.isEmpty()) runtime.getRecipeManager().addRecipes(RECIPE_TYPE, newRecipes);
        REGISTERED_RECIPES = newRecipes;
    }

    private static List<LootBoxJeiRecipe> buildRecipes() {
        return LootBoxManager.creativeDefinitions().stream()
                .map(definition -> new LootBoxJeiRecipe(
                        LootBoxItem.createStack(definition.id().toString()), definition.rolls(),
                        definition.entries(), LootBoxManager.jeiInfo(definition), definition.entries()))
                .toList();
    }

    private static final class Category implements IRecipeCategory<LootBoxJeiRecipe> {
        private final IDrawable icon;

        private Category(IGuiHelper guiHelper) {
            this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, LootBoxItem.createStack("lootbox:common"));
        }

        @Override public RecipeType<LootBoxJeiRecipe> getRecipeType() { return RECIPE_TYPE; }
        @Override public Component getTitle() { return Component.translatable("jei.lootbox.loot_box"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 180; }
        @Override public int getHeight() { return 148; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, LootBoxJeiRecipe recipe, IFocusGroup focuses) {
            builder.addInputSlot(6, 6)
                    .setStandardSlotBackground()
                    .addItemStack(recipe.box());
            for (LootBoxDefinition.Entry entry : recipe.entries()) {
                var slot = builder.addOutputSlot(0, 0);
                if (entry.tagId() != null) {
                    slot.addItemStacks(entry.resolvedStacks().stream()
                            .map(stack -> stack.copyWithCount(entry.min())).toList());
                } else {
                    slot.addItemStack(entry.stack().copyWithCount(entry.min()));
                }
                slot.addRichTooltipCallback((view, tooltip) -> {
                    if (entry.tagId() != null) {
                        tooltip.add(Component.translatable("jei.lootbox.tag_contents", entry.resolvedStacks().size()));
                    }
                    tooltip.add(Component.translatable("jei.lootbox.quantity", quantityText(entry)));
                    tooltip.add(Component.translatable("jei.lootbox.weight", formatNumber(entry.weight())));
                    tooltip.add(Component.translatable("jei.lootbox.luck_weight", formatNumber(entry.luckWeight())));
                    tooltip.add(Component.translatable("jei.lootbox.final_probability", formatProbability(recipe, entry)));
                    Component condition = entry.conditionComponent().getString().isBlank()
                            ? Component.translatable("jei.lootbox.condition.none")
                            : entry.conditionComponent();
                    tooltip.add(Component.translatable("jei.lootbox.condition", condition));
                });
            }
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, LootBoxJeiRecipe recipe, IFocusGroup focuses) {
            builder.addScrollGridWidget(
                    builder.getRecipeSlots().getSlots(RecipeIngredientRole.OUTPUT), 6, 3)
                    .setPosition(38, 6);
        }

        @Override
        public void draw(LootBoxJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            var font = Minecraft.getInstance().font;
            int y = 91;
            graphics.drawString(font, Component.translatable("tooltip.lootbox.loot_box_rolls", recipe.rolls()), 4, y, 0x404040, false);
            y += font.lineHeight + 2;
            for (Component info : recipe.info()) {
                List<FormattedCharSequence> infoLines = font.split(info, getWidth() - 8);
                for (FormattedCharSequence line : infoLines) {
                    graphics.drawString(font, line, 4, y, 0x606060, false);
                    y += font.lineHeight;
                }
            }
            List<FormattedCharSequence> hintLines = font.split(
                    Component.translatable("jei.lootbox.loot_box_hint", formatNumber(currentLuck())), getWidth() - 8);
            for (int line = 0; line < hintLines.size(); line++) {
                graphics.drawString(font, hintLines.get(line), 4, y + line * font.lineHeight, 0x777777, false);
            }
        }

        private static String quantityText(LootBoxDefinition.Entry entry) {
            return entry.min() == entry.max() ? Integer.toString(entry.min())
                    : entry.min() + "-" + entry.max();
        }

        private static String formatNumber(double value) {
            return String.format(Locale.ROOT, "%.2f", value);
        }

        private static float currentLuck() {
            var player = Minecraft.getInstance().player;
            return player == null ? 0.0F : player.getLuck();
        }

        private static double effectiveWeight(LootBoxDefinition.Entry entry, float luck) {
            return Math.max(0.0D, entry.weight() + luck * entry.luckWeight());
        }

        private static boolean availableAtLuck(LootBoxDefinition.Entry entry, float luck) {
            return entry.luckMinimum() == null || luck >= entry.luckMinimum();
        }

        private static String formatProbability(LootBoxJeiRecipe recipe, LootBoxDefinition.Entry entry) {
            float luck = currentLuck();
            if (!availableAtLuck(entry, luck)) return "0.00%";
            double total = recipe.probabilityEntries().stream()
                    .filter(candidate -> availableAtLuck(candidate, luck))
                    .mapToDouble(candidate -> effectiveWeight(candidate, luck))
                    .sum();
            double probability = total <= 0.0D ? 0.0D : effectiveWeight(entry, luck) / total;
            return String.format(Locale.ROOT, "%.2f%%", probability * 100.0D);
        }
    }
}
