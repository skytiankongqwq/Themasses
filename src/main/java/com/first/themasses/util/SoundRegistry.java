package com.first.themasses.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundRegistry {
    // 1. 创建注册表
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "themasses");

    // 2. 注册baizeli技能音频
    public static final RegistryObject<SoundEvent> BAIZELI_SKILL = SOUNDS.register("haqi",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("themasses", "haqi")));

    // 记得在你的主类 Mod 构造函数中调用 SOUNDS.register(bus);
}