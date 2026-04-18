package com.first.themasses.events.ironspell;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 饰品法术识别：通过反射实现兼容，允许在 Curios 槽位中的法术容器显示在法术轮盘上
 */
@Mod.EventBusSubscriber(modid = "themasses")
public class SpellScannerEvent {

    @SubscribeEvent
    public static void onSpellSelectionInit(net.minecraftforge.eventbus.api.Event event) {
        // 1. 运行时检查：如果没装 mod，直接走人
        if (!ModList.get().isLoaded("irons_spellbooks")) {
            return;
        }

        // 2. 检查 Event 的类名。避免直接使用 instanceof 触发类加载
        if (event.getClass().getName().equals("io.redspace.ironsspellbooks.api.magic.SpellSelectionManager$SpellSelectionEvent")) {
            IronsSpellScannerReflectiveCompat.handle(event);
        }
    }

    private static class IronsSpellScannerReflectiveCompat {

        public static void handle(Object eventObj) {
            try {
                // 获取 Event 实例中的 Player 对象
                Method getEntityMethod = eventObj.getClass().getMethod("getEntity");
                LivingEntity player = (LivingEntity) getEntityMethod.invoke(eventObj);

                // 获取 addSelectionOption 方法供后续使用
                // 参数类型为: (SpellData, slotName, localSlotIndex)
                // 注意：这里由于 SpellData 也是第三方类，我们之后也需要反射获取它
                Method addOptionMethod = eventObj.getClass().getMethod("addSelectionOption",
                        Class.forName("io.redspace.ironsspellbooks.api.spells.SpellData"), String.class, int.class);

                // 扫描 Curios 槽位
                top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                    // 这里以 "mysterious" 槽位为例
                    handler.getStacksHandler("mysterious").ifPresent(stacksHandler -> {
                        for (int i = 0; i < stacksHandler.getSlots(); i++) {
                            ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                            if (!stack.isEmpty()) {
                                processStack(stack, eventObj, addOptionMethod, i);
                            }
                        }
                    });
                });
            } catch (Exception e) {
                // 静默失败，或者记录日志
                // e.printStackTrace();
            }
        }

        private static void processStack(ItemStack stack, Object eventObj, Method addOptionMethod, int slotIdx) {
            try {
                // 反射调用 ISpellContainer.isSpellContainer(stack)
                Class<?> containerClass = Class.forName("io.redspace.ironsspellbooks.api.spells.ISpellContainer");
                Method isContainerMethod = containerClass.getMethod("isSpellContainer", ItemStack.class);

                if ((boolean) isContainerMethod.invoke(null, stack)) {
                    // 获取 Container 实例
                    Method getContainerMethod = containerClass.getMethod("get", ItemStack.class);
                    Object container = getContainerMethod.invoke(null, stack);

                    // 检查 spellWheel() 是否开启
                    Method spellWheelMethod = container.getClass().getMethod("spellWheel");
                    if ((boolean) spellWheelMethod.invoke(container)) {

                        // 获取法术列表 List<SpellData>
                        Method getActiveSpellsMethod = container.getClass().getMethod("getActiveSpells");
                        List<?> activeSpells = (List<?>) getActiveSpellsMethod.invoke(container);

                        for (int j = 0; j < activeSpells.size(); j++) {
                            // 调用 event.addSelectionOption(spellData, "mysterious", j)
                            addOptionMethod.invoke(eventObj, activeSpells.get(j), "mysterious", j);
                        }
                    }
                }
            } catch (Exception ignored) {
                // 某个环节失败说明该物品不是合法的法术容器
            }
        }
    }
}