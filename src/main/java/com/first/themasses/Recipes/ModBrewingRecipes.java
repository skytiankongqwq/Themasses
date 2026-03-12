package com.first.themasses.Recipes;

import com.first.themasses.effect.ModPotions;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
//药水酿造注册
@Mod.EventBusSubscriber(modid = "themasses", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBrewingRecipes {
    // 1. 注册核心配方：水瓶 + 绿宝石块 -> 喷溅药水
    @SubscribeEvent
    public static void registerBrewingRecipes(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {

            // 输出 (Output): 带有 BAMBOOZLED_POTION 效果的喷溅药水
            ItemStack output = PotionUtils.setPotion(
                    new ItemStack(Items.SPLASH_POTION),
                    ModPotions.BAMBOOZLED_POTION.get()
            );

            // 输入 Stack (水瓶)
            ItemStack waterBottleStack = PotionUtils.setPotion(
                    new ItemStack(Items.POTION),
                    Potions.WATER
            );
            // 转换为 Ingredient
            Ingredient inputIngredient = Ingredient.of(waterBottleStack);

            // 催化剂 (Ingredient): 绿宝石块
            Ingredient catalystIngredient = Ingredient.of(Items.EMERALD_BLOCK);

            // 注册配方
            BrewingRecipeRegistry.addRecipe(
                    inputIngredient,  // 输入 (水瓶)
                    catalystIngredient, // 催化剂 (绿宝石块)
                    output            // 输出 (喷溅药水)
            );
        });
    }
}