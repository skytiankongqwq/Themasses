package com.first.themasses.attributes;

import com.first.themasses.TheMasses;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 自定义属性注册。从 Dream Sakura 的 attributes 包移植，命名空间改为 themasses。
 */
public class RegistryAttribute {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, TheMasses.MOD_ID);

    public static final RegistryObject<Attribute> SCALE = ATTRIBUTES.register(
            "scale",
            () -> new RangedAttribute("attribute.themasses.scale", 1.0D, 0.0D, 64.0D).setSyncable(true)
    );
}
