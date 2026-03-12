package com.first.themasses.util;

import com.first.themasses.TheMasses;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.lang.reflect.Field;
//解锁一些属性上限
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AttributeUnlocker {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 解锁常见的几种属性上限
            unlockLimit(Attributes.MAX_HEALTH);
            unlockLimit(Attributes.ATTACK_DAMAGE);
            unlockLimit(Attributes.ARMOR);
            unlockLimit(Attributes.ARMOR_TOUGHNESS);
            unlockLimit(Attributes.LUCK);
            unlockLimit(Attributes.ATTACK_SPEED);
        });
    }

    private static void unlockLimit(Attribute attribute) {
        if (attribute instanceof RangedAttribute ranged) {
            String[] fieldNames = {"maxValue", "f_22223_", "field_22223_"};
            boolean success = false;

            for (String name : fieldNames) {
                try {
                    Field field = RangedAttribute.class.getDeclaredField(name);
                    field.setAccessible(true);
                    field.setDouble(ranged, 102400000000000000.0);
                    success = true;
                    break; // 找到一个成功的就跳出循环
                } catch (NoSuchFieldException ignored) {
                    // 继续尝试下一个名字
                } catch (Exception e) {
                    TheMasses.LOGGER.error("设置属性 {} 时发生意外错误", name, e);
                }
            }

            if (success) {
                System.out.println("已成功解锁属性上限: " + attribute.getDescriptionId());
            } else {
                TheMasses.LOGGER.error("未能找到属性上限字段，无法解锁: " + attribute.getDescriptionId());
            }
        }
    }
}