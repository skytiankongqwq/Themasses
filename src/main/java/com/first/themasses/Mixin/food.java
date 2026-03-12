package com.first.themasses.Mixin;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

//大理石()
@Mixin(Item.class)
public interface food {

    @Accessor("foodProperties")
    FoodProperties getFoodProperties();

    @Accessor("foodProperties")
    @Mutable
    void setFoodProperties(FoodProperties foodProperties);
}
