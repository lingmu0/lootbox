package net.xuwu.lootbox;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.ingredients.subtypes.UidContext;
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

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(LootBoxMod.MODID, "jei_plugin");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(LootBoxMod.LOOT_BOX.get(), new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<>() {
            @Override public Object getSubtypeData(ItemStack stack, UidContext context) {
                return LootBoxItem.getDefinitionId(stack).toString();
            }
            @Override public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                return LootBoxItem.getDefinitionId(stack).toString();
            }
        });
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<LootBoxJeiRecipe> recipes = LootBoxManager.definitions().values().stream()
                .filter(definition -> !LootBoxConfig.HIDE_DEFAULT_BOXES.get()
                        || !LootBoxManager.isDefaultBox(definition.id()))
                .map(definition -> new LootBoxJeiRecipe(
                        LootBoxItem.createStack(definition.id().toString()), definition.rolls(), definition.entries()))
                .toList();
        if (!recipes.isEmpty()) registration.addRecipes(RECIPE_TYPE, recipes);
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
        @Override public int getHeight() { return 126; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, LootBoxJeiRecipe recipe, IFocusGroup focuses) {
            builder.addInputSlot(6, 6).addItemStack(recipe.box());
            int index = 0;
            for (LootBoxDefinition.Entry entry : recipe.entries()) {
                int row = index / 6;
                int column = index % 6;
                if (row >= 3) break;
                var slot = builder.addOutputSlot(38 + column * 23, 6 + row * 23)
                        .addItemStack(entry.stack().copyWithCount(entry.min()));
                slot.addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.translatable("jei.lootbox.quantity", quantityText(entry)));
                    tooltip.add(Component.translatable("jei.lootbox.weight", formatNumber(entry.weight())));
                    tooltip.add(Component.translatable("jei.lootbox.luck_weight", formatNumber(entry.luckWeight())));
                    tooltip.add(Component.translatable("jei.lootbox.final_probability", formatProbability(recipe, entry)));
                    Component condition = entry.conditionComponent().getString().isBlank()
                            ? Component.translatable("jei.lootbox.condition.none")
                            : entry.conditionComponent();
                    tooltip.add(Component.translatable("jei.lootbox.condition", condition));
                });
                index++;
            }
        }

        @Override
        public void draw(LootBoxJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.translatable("tooltip.lootbox.loot_box_rolls", recipe.rolls()), 4, 91, 0x404040, false);
            List<FormattedCharSequence> hintLines = font.split(
                    Component.translatable("jei.lootbox.loot_box_hint", formatNumber(currentLuck())), getWidth() - 8);
            for (int line = 0; line < hintLines.size(); line++) {
                graphics.drawString(font, hintLines.get(line), 4, 103 + line * font.lineHeight, 0x777777, false);
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
            double total = recipe.entries().stream()
                    .filter(candidate -> availableAtLuck(candidate, luck))
                    .mapToDouble(candidate -> effectiveWeight(candidate, luck))
                    .sum();
            double probability = total <= 0.0D ? 0.0D : effectiveWeight(entry, luck) / total;
            return String.format(Locale.ROOT, "%.2f%%", probability * 100.0D);
        }
    }
}
