package com.first.themasses;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// 配置文件
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // 1. Boss 列表：改用 simpleValidateName 防止初始化冲突
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_BOSS_STRINGS = BUILDER
            .comment("在此列表中添加生物 ID。系统会强制将这些生物判定为 Boss。",
                    "bosses_list = [",
                    "    \"minecraft:warden\",",
                    "    \"minecraft:zombie\"",
                    "]")
            .defineListAllowEmpty("bosses_list", List.of("minecraft:warden"), Config::simpleValidateName);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // 内存中的变量，方便其他类直接调用
    public static Set<Item> items;
    public static Set<EntityType<?>> customBosses;

    /**
     * 简单的格式验证器：仅检查是否为有效的 ResourceLocation 格式。
     * 这样可以避开在配置初始化阶段注册表尚未就绪的问题。
     */
    private static boolean simpleValidateName(final Object obj) {
        return obj instanceof String name && ResourceLocation.tryParse(name) != null;
    }

    @Deprecated
    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(ResourceLocation.tryParse(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // 同步 Boss
        customBosses = CUSTOM_BOSS_STRINGS.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(loc -> loc != null && ForgeRegistries.ENTITY_TYPES.containsKey(loc))
                .map(ForgeRegistries.ENTITY_TYPES::getValue)
                .collect(Collectors.toSet());
    }
}