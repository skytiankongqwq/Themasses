package com.first.themasses.effect;

import com.first.themasses.TheMasses;
import com.first.themasses.effect.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
//药水瓶注册
public class ModPotions {

    // 注册表：用于注册新的 Potion 对象
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, TheMasses.MOD_ID);

    public static final RegistryObject<Potion> BAMBOOZLED_POTION = POTIONS.register("bamboozled_potion",
            () -> new Potion(new MobEffectInstance(
                    ModEffects.BAMBOOZLED.get(),
                    600, // 持续时间
                    0 // 等级
            ))
    );

    public static void register(IEventBus bus) {
        POTIONS.register(bus);
    }
}