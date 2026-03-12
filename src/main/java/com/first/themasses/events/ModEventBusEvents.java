package com.first.themasses.events;

import com.first.themasses.entity.ModEntities;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 确保你的 MOD_ID 是正确的
import static com.first.themasses.TheMasses.MOD_ID;
//实体属性
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void entityAttributeEvent(EntityAttributeCreationEvent event) {

        // 💥 注册 FlashCreeper 的属性
        event.put(ModEntities.FLASH_CREEPER.get(), setFlashCreeperAttributes());
    }

    private static AttributeSupplier setFlashCreeperAttributes() {
        return Creeper.createAttributes()
                // 💥 1000万血量
                .add(Attributes.MAX_HEALTH, 10000000.0D)
                // 💥 超快移速
                .add(Attributes.MOVEMENT_SPEED, 10)
                .build();
    }
}