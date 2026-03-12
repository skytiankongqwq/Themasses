package com.first.themasses.item.prop;

import com.first.themasses.events.prop.BlessingEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import java.util.Random;
//p7物品的概率代码
public class BlessingEngine {
    private static final Random RANDOM = new Random();

    public static void applyBlessing(Player player, int chargeTime) {
        float roll = RANDOM.nextFloat() * 100f;
        float progress = Math.min(chargeTime / 200.0f, 1.0f); // 假设最高蓄力200tick

        // --- 核心修复：调用统一清理方法 ---
        BlessingEvents.clearAllBlessingData(player);

        float probFinal = 1.0f + (19.0f * progress);
        float probMyth = 13.0f + (47.0f * progress);
        float probEpic = 30.0f - (20.0f * progress);
        float probCommon = 51.0f - (41.0f * progress);
        float probDistorted = 3.0f;
        float probCurse = 2.0f;

        if (roll < probFinal) {
            applyFinal(player);
        } else if (roll < (probFinal + probMyth)) {
            applyMyth(player);
        } else if (roll < (probFinal + probMyth + probEpic)) {
            applyEpic(player);
        } else if (roll < (probFinal + probMyth + probEpic + probDistorted)) {
            applyDistorted(player);
        } else if (roll < (probFinal + probMyth + probEpic + probDistorted + probCurse)) {
            applyCurse(player);
        } else {
            applyCommon(player);
        }
    }

    private static void applyFinal(Player player) {
        // 留空，待后续开发
        player.getPersistentData().putBoolean("P7_Final_Active", true);
        player.sendSystemMessage(Component.literal("§d§l【终之祈】§f 这是最终的一击..."));
    }

    private static void applyCommon(Player player) {
        int type = RANDOM.nextInt(7);
        String name = "";
        String desc = "";

        switch (type) {
            case 0 -> { name = "千斩"; desc = "攻击时概率追加额外斩击"; }
            case 1 -> { name = "连斩"; desc = "击杀目标后攻速翻倍(持续1min)"; }
            case 2 -> {
                name = "破甲"; desc = "伤害提升30%，持续5s";
                player.getPersistentData().putInt("P7_Common_Timer", 100);
            }
            case 3 -> {
                name = "水行"; desc = "水下移速+50%，持续2min";
                player.getPersistentData().putInt("P7_Common_Timer", 2400);
            }
            case 4 -> { name = "精准"; desc = "下3次攻击提升伤害"; }
            case 5 -> {
                name = "夜视"; desc = "获得夜视，持续3min";
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 3600, 0));
                player.getPersistentData().putInt("P7_Common_Timer", 3600);
            }
            case 6 -> {
                name = "迅捷"; desc = "移速+30%，持续40s";
                player.getPersistentData().putInt("P7_Common_Timer", 800);
            }
        }
        player.getPersistentData().putInt("P7_Common", type + 1);
        sendFormattedMsg(player, "§f", "普通祈福", name, desc);
    }

    private static void applyEpic(Player player) {
        int type = RANDOM.nextInt(5);
        String name = "";
        String desc = "";

        switch (type) {
            case 0 -> {
                name = "嗜血"; desc = "10s内击杀恢复5%生命";
                player.getPersistentData().putInt("P7_Epic_Timer", 200);
            }
            case 1 -> {
                name = "金刚"; desc = "免疫击退，持续15s";
                player.getPersistentData().putInt("P7_Epic_Timer", 300);
            }
            case 2 -> {
                name = "破军"; desc = "攻击造成溅射伤害，持续10s";
                player.getPersistentData().putInt("P7_Epic_Timer", 200);
            }
            case 3 -> {
                name = "无双"; desc = "孤军奋战时伤害+60%，持续20s";
                player.getPersistentData().putInt("P7_Epic_Timer", 400);
            }
            case 4 -> {
                name = "隐身伏击"; desc = "隐身5s，破隐第一击200%伤害";
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0));
                // 持续时间交由 handleEffectTimers 管理
                player.getPersistentData().putInt("P7_Epic_Timer", 100);
            }
        }
        player.getPersistentData().putInt("P7_Epic", type + 1);
        sendFormattedMsg(player, "§6", "强化祈福", name, desc);
    }

    private static void applyMyth(Player player) {
        int type = RANDOM.nextInt(3);
        String name = "";
        String desc = "";

        switch (type) {
            case 0 -> {
                name = "洁净之躯"; desc = "60s内免疫所有状态";
                player.removeAllEffects();
                player.getPersistentData().putInt("P7_Myth_Timer", 1200);
            }
            case 1 -> {
                name = "死亡宣告"; desc = "锁定目标，30s内对其伤害+500%";
                player.getPersistentData().remove("P7_DeclaredTarget"); // 确保重置锁定
                player.getPersistentData().putInt("P7_Myth_Timer", 600);
            }
            case 2 -> {
                name = "绝对防御"; desc = "5s内无敌，但无法攻击";
                player.getPersistentData().putInt("P7_Myth_Timer", 100);
            }
        }
        player.getPersistentData().putInt("P7_Myth", type + 1);
        sendFormattedMsg(player, "§b", "神话祈福", name, desc);
    }

    private static void applyDistorted(Player player) {
        int type = RANDOM.nextInt(3);
        String name = "";
        String desc = "";

        switch (type) {
            case 0 -> { name = "血肉流失"; desc = "攻速+100%,吸血+30% | 负效:脆弱不堪的身体和荆棘的武器"; }
            case 1 -> { name = "扭曲空间"; desc = "攻击距离极大提升 | 负效:空间位置波动"; }
            case 2 -> { name = "被诅咒者"; desc = "伤害倍增*10 | 负效:受击必死"; }
        }
        player.getPersistentData().putInt("P7_Distorted", type + 1);
        sendFormattedMsg(player, "§5", "扭曲祈福", name, desc);
    }

    private static void applyCurse(Player player) {
        int type = RANDOM.nextInt(3);
        String name = "";
        String desc = "";

        switch (type) {
            case 0 -> { name = "生命献祭"; desc = "持续燃烧生命，直到死亡"; }
            case 1 -> { name = "脆弱之躯"; desc = "受到伤害会不断累加"; }
            case 2 -> { name = "刃之回响"; desc = "造成伤害的120%反弹自身"; }
        }
        player.getPersistentData().putInt("P7_Curse", type + 1);
        sendFormattedMsg(player, "§4", "诅咒祈福", name, desc);
    }

    private static void sendFormattedMsg(Player player, String color, String tier, String name, String desc) {
        player.sendSystemMessage(Component.literal(color + "§l[" + tier + "] §f获得: §n" + name));
        player.sendSystemMessage(Component.literal("§7效果: " + desc));
    }
}