package com.first.themasses.events.ironspell;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

/**
 * 属性补丁：将王冠原本在 head 槽位的属性加成同步到自定义的 crown 槽位
 */
@Mod.EventBusSubscriber(modid = "themasses")
public class IronCrownAttachEvent {

    @SubscribeEvent
    public static void onCurioAttribute(CurioAttributeModifierEvent event) {
        // 1. 守卫判断
        if (!ModList.get().isLoaded("curios") || !ModList.get().isLoaded("irons_spellbooks")) {
            return;
        }

        // 2. 核心修复：Curios 事件中使用 getSlotContext().identifier() 获取槽位名
        if ("crown".equals(event.getSlotContext().identifier())) {
            ItemStack stack = event.getItemStack();

            if (!stack.isEmpty()) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());

                // 3. 判定是否为 Iron's Spellbooks 的王冠类物品
                if (id != null && "irons_spellbooks".equals(id.getNamespace())) {
                    String path = id.getPath();
                    if (path.equals("gold_crown") || path.equals("tarnished_crown") || path.equals("dev_crown")) {

                        // 4. 获取物品在 HEAD 槽位的所有默认属性加成（法强、冷却等）
                        var modifiers = stack.getAttributeModifiers(EquipmentSlot.HEAD);

                        // 5. 注入到当前的 Curios 属性列表中
                        modifiers.forEach(event::addModifier);
                    }
                }
            }
        }
    }
}