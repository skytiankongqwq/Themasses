package com.first.themasses.item.food; // 🚨 确保你把这个包名改成你项目里实际的包名！

import com.first.themasses.Mixin.food;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

//可食用的大理石)
public class goetyitem {

    private static final String GOETY_MOD_ID = "goety";
    private static final String MARBLE_ID = GOETY_MOD_ID + ":marble";
    private static final String SILT_ID = GOETY_MOD_ID + ":silt_marble_heavy";

    // **吃石头的食物属性** (饥饿值 2, 饱和度 1.0)
    private static final FoodProperties GOETY_MARBLE_FOOD = new FoodProperties.Builder()
            .nutrition(2)       // 2 点饥饿值
            .saturationMod(0.5F) // 0.5 * 2 = 1.0 饱和度
            .build();

    public static boolean isGoetyLoaded() {
        return ModList.get().isLoaded(GOETY_MOD_ID);
    }

    public static void registerGoetyFoodProperties() {
        if (!isGoetyLoaded()) {
            return;
        }

        System.out.println("[GoetyIntegration] 💥 Goety 已加载，尝试把石头变成食物！");

        modifyItemFoodProperty(MARBLE_ID);
        modifyItemFoodProperty(SILT_ID);
    }

    private static void modifyItemFoodProperty(String itemId) {
        try {
            Item targetItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemId));

            if (targetItem == null) {
                System.out.println("警告：找不到 Goety 的物品 ID: " + itemId);
                return;
            }


            if (targetItem instanceof food) {
                ((food) targetItem).setFoodProperties(GOETY_MARBLE_FOOD);
                System.out.println("✅ 成功把 [" + itemId + "] 设置为食物！");
            } else {
                System.err.println("❌ Mixin 未能成功应用到 Item 类，无法修改: " + itemId);
            }

        } catch (Exception e) {
            System.err.println("❌ 未知错误，Goety 物品修改失败: " + itemId);
            e.printStackTrace();
        }
    }
}


