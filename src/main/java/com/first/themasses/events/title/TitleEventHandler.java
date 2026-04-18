package com.first.themasses.events.title;

import com.first.themasses.TheMasses;
import com.first.themasses.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID)
public class TitleEventHandler {

    /**
     * 核心逻辑：修改玩家的显示名称
     * 这样聊天框、头顶名字、Tab 栏都会自动同步，且头衔在最前面
     */
    @SubscribeEvent
    public static void onPlayerNameFormat(PlayerEvent.NameFormat event) {
        Player player = event.getEntity();
        Component title = buildColoredTitle(player);
        // 1. 获取名字组件（变身名或原名）
        // 这里直接取 getHuanpiName 返回的文本组件
        Component huanpiName = getHuanpiName(player);
        // 2. 如果既没头衔也没改名，直接跳过，不操作任何样式
        if (title == null && huanpiName.equals(player.getName())) {
            return;
        }
        MutableComponent newName = Component.empty();
        // 3. 拼接头衔（头衔本身可能带有特殊颜色，这里保留）
        if (title != null) {
            newName.append(title);
            newName.append(Component.literal(" ")); // 仅添加空格分隔
        }
        newName.append(huanpiName);
        // 设置为新的显示名称
        event.setDisplayname(newName);
    }

    /**
     * Tab 列表格式化：确保 Tab 栏也同步
     */
    @SubscribeEvent
    public static void onTabListName(PlayerEvent.TabListNameFormat event) {
        // 由于上面 NameFormat 已经改了名字，这里直接获取即可
        event.setDisplayName(event.getEntity().getDisplayName());
    }

    // --- 辅助方法保持不变 ---

    private static Component getHuanpiName(Player player) {
        if (player.getPersistentData().contains("HuanpiName")) {
            String huanpiName = player.getPersistentData().getString("HuanpiName");
            if (!huanpiName.isEmpty()) {
                return Component.literal(huanpiName);
            }
        }
        return player.getName();
    }

    private static Component buildColoredTitle(Player player) {
        String titleText = "";
        String colorStr = "white";

        if (player.getPersistentData().contains("CustomTitle")) {
            titleText = player.getPersistentData().getString("CustomTitle");
            colorStr = player.getPersistentData().getString("CustomTitleColor");
        }

        if (CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.LAOWANG237.get()).isPresent()) {titleText = "[baka]";colorStr = "#55FFFF";}
        if (CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.WU1WU2.get()).isPresent()) {titleText = "[wu1wu2]";colorStr = "#9400D3";}

        if (titleText.isEmpty()) return null;

        TextColor color = TextColor.parseColor(colorStr);
        if (color == null) color = TextColor.fromRgb(0xFFFFFF);

        return Component.literal(titleText).withStyle(Style.EMPTY.withColor(color));
    }
}