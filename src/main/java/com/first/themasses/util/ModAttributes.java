package com.first.themasses.util;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
        DeferredRegister.create(ForgeRegistries.ATTRIBUTES, "themasses");

    // 模仿 GOLEM_SIZE 的逻辑：基础值为 1.0 (原大小)，同步到客户端
    public static final RegistryObject<Attribute> MIKU_SIZE = ATTRIBUTES.register("miku_size",
            () -> new RangedAttribute("attribute.name.themasses.miku_size", 1.0D, 0.0D, 1024.0D).setSyncable(true));

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }
}
