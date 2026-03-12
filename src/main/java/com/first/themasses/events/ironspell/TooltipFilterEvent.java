package com.first.themasses.events.ironspell;

import com.first.themasses.curios.mysterious.DaZhi;
import com.first.themasses.curios.mysterious.Kanghuoyaoshui;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//删文本用的
@Mod.EventBusSubscriber(modid = "themasses")
public class TooltipFilterEvent {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof DaZhi) {
            // 移除所有不是你自己添加的描述行
            // 我们通过排除法：只保留第一行(名字)和你自定义的那一行(翻译键包含 dazhi)
            event.getToolTip().removeIf(component -> {
                String text = component.getString();
                // 1. 绝对不要删掉第一行物品名称
                if (text.equals(stack.getHoverName().getString())) return false;
                // 2. 保留你自己的紫色文本 (假设你的翻译键里有 dazhi)
                if (text.contains("抱走了") || text.contains("dazhi")) return false;
                // 3. 保留 Curios 原生的“饰品加成”标题和属性 (如果你想留着的话)
                if (text.contains("饰品加成") || text.contains("方块范围")) return false;
                // 4. 剩下的全部干掉：
                return true;
            });
        }
        // --- 新增：针对 Kanghuoyaoshui 的过滤逻辑 ---
        else if (stack.getItem() instanceof Kanghuoyaoshui) {
            event.getToolTip().removeIf(component -> {
                String text = component.getString();
                // 1. 保留物品名称
                if (text.equals(stack.getHoverName().getString())) return false;
                // 2. 保留你自定义的翻译行
                if (text.contains("这其实是瓶药水") || text.contains("green_hand")) return false;
                // 3. 【核心修改】删掉空行或只包含空白字符的行
                if (text.trim().isEmpty()) return true;
                // 4. 拦截 Curios 自动生成的文本
                if (text.contains("栏位") ||
                        text.contains("Slot") ||
                        text.contains("Curio") ||
                        text.contains("饰品加成") ||
                        (text.contains("在") && text.contains("槽位时")) ||
                        text.startsWith(" ") ||
                        text.startsWith("+")) {
                    return true;
                }
                return false;
            });
        }
    }
}