package com.first.themasses.events.ironspell;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * 饰品法术识别：允许在 Curios 槽位中的法术容器显示在法术轮盘上
 */
@Mod.EventBusSubscriber(modid = "themasses")
public class SpellScannerEvent {

    @SubscribeEvent
    public static void onSpellSelectionInit(net.minecraftforge.eventbus.api.Event event) {
        // 运行时守卫：未安装 Iron's Spellbooks 则直接退出
        if (!ModList.get().isLoaded("irons_spellbooks")) {
            return;
        }

        // 只有 Mod 存在时才进行 instanceof 检查，防止 JVM 尝试解析不存在的类
        if (event instanceof io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.SpellSelectionEvent spellEvent) {
            IronsSpellScannerCompat.handleSpellScanning(spellEvent);
        }
    }

    /**
     * 内部兼容处理逻辑，确保第三方类引用被隔离
     */
    private static class IronsSpellScannerCompat {

        private static void handleSpellScanning(io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.SpellSelectionEvent event) {
            var player = event.getEntity();

            // 扫描 "mysterious" 槽位 (或根据需要修改为 "crown")
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.getStacksHandler("mysterious").ifPresent(stacksHandler -> {
                    for (int i = 0; i < stacksHandler.getSlots(); i++) {
                        var stack = stacksHandler.getStacks().getStackInSlot(i);

                        // 检查并处理法术容器
                        if (!stack.isEmpty() && io.redspace.ironsspellbooks.api.spells.ISpellContainer.isSpellContainer(stack)) {
                            var container = io.redspace.ironsspellbooks.api.spells.ISpellContainer.get(stack);

                            if (container.spellWheel()) {
                                var activeSpells = container.getActiveSpells();
                                for (int j = 0; j < activeSpells.size(); j++) {
                                    // 将法术添加到选择轮盘
                                    event.addSelectionOption(activeSpells.get(j), "mysterious", j);
                                }
                            }
                        }
                    }
                });
            });
        }
    }
}