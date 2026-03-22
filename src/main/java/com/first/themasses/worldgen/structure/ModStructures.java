package com.first.themasses.worldgen.structure;

import com.first.themasses.TheMasses;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, TheMasses.MOD_ID);

    // 这一步把 "monastic_courtyard" 字符串关联到你的代码类
    public static final RegistryObject<StructureType<MonasticCourtyardStructure>> MONASTIC_COURTYARD =
            STRUCTURE_TYPES.register("monastic_courtyard", () -> () -> MonasticCourtyardStructure.CODEC);

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
    }
}