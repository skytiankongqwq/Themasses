package com.first.themasses.item;

import com.first.themasses.effect.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;

//食物注册
public class ModFoods {

    public static final FoodProperties LAOWANG_FOOD_PROPERTIES = new FoodProperties.Builder()
            .nutrition(20)
            .saturationMod(20.0F)
            .alwaysEat()
            .build();

    public static final FoodProperties SPIRIT_LIQUID_VIAL = new FoodProperties.Builder()
            .nutrition(10)
            .saturationMod(10F)
            .alwaysEat()
            .build();

    public static final FoodProperties FORBIDDEN_FRUIT_OF_EDEN = new FoodProperties.Builder()
            .nutrition(10)          // 4点饱食度
            .saturationMod(10f)   // 0.3f 饱和度修正值
            .alwaysEat()
            .build();
    public static final FoodProperties DOOF_GNAWOAL = new FoodProperties.Builder()
            .nutrition(4)          // 恢复 2 格饥饿度
            .saturationMod(0.3f)   // 饱和度
            .alwaysEat()           // 满血满饥饿也能吃（方便测试）
            .effect(() -> new MobEffectInstance(ModEffects.PARADOX_VIRUS.get(), 600, 0), 1.0F)
            .build();

    // 可以在这里添加其他的食物属性，例如：
    // public static final FoodProperties EXAMPLE_BERRY = new FoodProperties.Builder().nutrition(1).saturationMod(0.2F).build();
}
