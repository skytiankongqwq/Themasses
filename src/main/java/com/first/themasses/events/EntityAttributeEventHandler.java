package com.first.themasses.events;

import com.first.themasses.TheMasses;
import com.first.themasses.attributes.RegistryAttribute;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 把所有生物类型都挂上 themasses:scale 属性。
 * 从 Dream Sakura 的 EntityAttributeEventHandler 移植。
 */
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityAttributeEventHandler {

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> entityType : event.getTypes()) {
            if (!event.has(entityType, RegistryAttribute.SCALE.get())) {
                event.add(entityType, RegistryAttribute.SCALE.get());
            }
        }
    }
}
