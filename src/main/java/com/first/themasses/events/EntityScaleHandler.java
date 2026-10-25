package com.first.themasses.events;

import com.first.themasses.TheMasses;
import com.first.themasses.attributes.ScaleAttributeHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 让体型属性真正影响碰撞箱与视高。
 * 从 Dream Sakura 的 EntityEventHandler 移植。
 */
@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityScaleHandler {

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity livingEntity) {
            double scale = ScaleAttributeHelper.getScale(livingEntity);
            if (!ScaleAttributeHelper.isDefault(scale)) {
                float scaleF = (float) scale;
                event.setNewEyeHeight(event.getNewEyeHeight() * scaleF);
                event.setNewSize(event.getNewSize().scale(scaleF));
            }
        }
    }
}
