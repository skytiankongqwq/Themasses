package com.first.themasses;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static Set<String> ironSpellbooksEntityIds = new java.util.HashSet<>();
    public static Set<String> accurateEntityIds = new java.util.HashSet<>();
    public static Set<String> accurateLvl3EntityIds = new java.util.HashSet<>();
    public static Set<String> accurateLvl4EntityIds = new java.util.HashSet<>();
    public static Set<String> accurateLvl5EntityIds = new java.util.HashSet<>();
    public static Set<String> accurateLvl7EntityIds = new java.util.HashSet<>();
    public static Set<String> magicFocusEntityIds = new java.util.HashSet<>();
    public static Set<String> magicFocusLvl3EntityIds = new java.util.HashSet<>();

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_BOSS_STRINGS = BUILDER
            .comment("在此列表中添加生物 ID。系统会强制将这些生物判定为 Boss。")
            .defineListAllowEmpty("bosses_list", List.of("minecraft:warden"), Config::simpleValidateName);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> IRON_SPELLBOOKS_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ACCURATE_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ACCURATE_LVL3_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ACCURATE_LVL4_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ACCURATE_LVL5_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ACCURATE_LVL7_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MAGIC_FOCUS_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MAGIC_FOCUS_LVL3_ENTITIES;

    static {
        BUILDER.push("MikuBlessingSettings");

        IRON_SPELLBOOKS_ENTITIES = BUILDER
                .comment("拥有'铁魔法属性增益'特权的生物ID列表")
                .defineListAllowEmpty(List.of("iron_spellbooks_entities"), () -> Arrays.asList(
                        "goetyiron:deadking_servant",
                        "goetyiron:fire_boss_servant",
                        "goetyiron:priest_servant",
                        "goetyiron:cryomancer_servant",
                        "goetyiron:pyromancer_servant",
                        "goetyiron:archevoker_servant",
                        "goetyiron:ironspell_necromancer_servant"
                ), o -> o instanceof String);

        ACCURATE_ENTITIES = BUILDER
                .comment("基础'精准(最高2级)'额外buff的生物ID列表")
                .defineListAllowEmpty(List.of("accurate_entities"), () -> Arrays.asList(
                        "minecraft:player",
                        "minecraft:skeleton",
                        "minecraft:stray",
                        "goety:skeleton_pillager",
                        "goety:skeleton_servant",
                        "goety_ladder:void_skeleton",
                        "goety:mossy_skeleton_servant",
                        "goety:rattled_servant",
                        "goetyawaken:parched_servant",
                        "goety:sunken_skeleton_servant",
                        "goety:stray_servant",
                        "goety:wither_skeleton_servant",
                        "goety:bone_spider_servant",
                        "goety_cataclysm:elite_draugr_servant",
                        "jerotesvillage:carved_villager",
                        "guardvillagers:guard",
                        "goetyawaken:parched_necromancer_servant",
                        "goety:cairn_necromancer_servant",
                        "goety_cataclysm:draugr_necromancer_servant",
                        "goetytwilight:snow_queen_servant",
                        "goety_cataclysm:cindaria_servant",
                        "goety:pillager_servant",
                        "goety:iceologer_servant",
                        "goety:cryologer_servant",
                        "recruits:captain",
                        "recruits:scout",
                        "recruits:messenger",
                        "recruits:bowman",
                        "recruits:nomad",
                        "goetyiron:ironspell_necromancer_servant",
                        "goetyiron:archevoker_servant",
                        "goetyiron:pyromancer_servant",
                        "goetyiron:cryomancer_servant",
                        "goetyiron:priest_servant",
                        "goety:drowned_necromancer_servant",
                        "player_mobs:player_mob",
                        "modulargolems:metal_golem",
                        "modulargolems:humanoid_golem",
                        // ---- 新增区域 ----
                        "goety:wildfire",
                        "goety:blaze_servant",
                        "artificers_armory:skeleton_minion",
                        "artificers_armory:stray_minion",
                        "artificers_armory:sentry",
                        "artificers_armory:hoven_sentry",
                        "artificers_armory:mobile_gear"
                ), o -> o instanceof String);

        ACCURATE_LVL3_ENTITIES = BUILDER
                .comment("拥有'精准(初始2级，满级3级)'额外buff的生物ID列表")
                .defineListAllowEmpty(List.of("accurate_lvl3_entities"), () -> Arrays.asList(
                        "goetytwilight:skeleton_druid_servant",
                        "someillagerservants:mutant_wither_skeleton_servant"
                ), o -> o instanceof String);

        ACCURATE_LVL4_ENTITIES = BUILDER
                .comment("拥有'精准(初始3级，满级4级)'额外buff的生物ID列表")
                .defineListAllowEmpty(List.of("accurate_lvl4_entities"), () -> Arrays.asList(
                        "goety_spillage:funnybone",
                        "goety_spillage:sniper",
                        "goety_cataclysm:cindaria_servant",
                        "someillagerservants:heresiarch_servant",
                        "someillagerservants:illashooter_servant"
                ), o -> o instanceof String);

        ACCURATE_LVL5_ENTITIES = BUILDER
                .comment("拥有'精准(初始4级，满级5级)'额外buff的生物ID列表")
                .defineListAllowEmpty(List.of("accurate_lvl5_entities"), () -> Arrays.asList(
                        "goetyawaken:preacher_servant",
                        "goety:cryologer_servant",
                        "goety_mastery_of_magic:ice_monarch",
                        "goety:iceologer_servant",
                        "goetyawaken:maid_fairy_servant",
                        "mutantmonsters:mutant_snow_golem"
                ), o -> o instanceof String);

        ACCURATE_LVL7_ENTITIES = BUILDER
                .comment("拥有'精准(初始6级，满级7级)'额外buff的生物ID列表")
                .defineListAllowEmpty(List.of("accurate_lvl7_entities"), () -> Arrays.asList(
                        "someillagerservants:conquillager_servant"
                ), o -> o instanceof String);

        MAGIC_FOCUS_ENTITIES = BUILDER
                .comment("基础'魔力聚焦(最高2级)'额外buff的生物ID列表")
                .defineListAllowEmpty(List.of("magic_focus_entities"), () -> Arrays.asList(
                        "minecraft:witch",
                        "goetyawaken:skull_lord_servant",
                        "goety:wither_necromancer_servant",
                        "goety_ladder:void_necromancer_servant",
                        "goety:necromancer_servant",
                        "goety:mossy_necromancer_servant",
                        "goety:grave_golem",
                        "goety:witch_servant",
                        "goetyawaken:crone_servant",
                        "goetyawaken:wraith_necromancer_servant"
                ), o -> o instanceof String);

        MAGIC_FOCUS_LVL3_ENTITIES = BUILDER
                .comment("拥有'魔力聚焦(初始2级，满级3级)'额外buff的生物ID列表")
                .defineListAllowEmpty(List.of("magic_focus_lvl3_entities"), () -> Arrays.asList(
                        "someillagerservants:magispeller_servant",
                        "goetyawaken:minister_servant",
                        "goetyawaken:vizier_servant",
                        "goetyawaken:sorcerer_servant",
                        "goety:evoker_servant"
                ), o -> o instanceof String);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static Set<Item> items;
    public static Set<EntityType<?>> customBosses;

    private static boolean simpleValidateName(final Object obj) {
        return obj instanceof String name && ResourceLocation.tryParse(name) != null;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // 同步 Boss
        customBosses = CUSTOM_BOSS_STRINGS.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(loc -> loc != null && ForgeRegistries.ENTITY_TYPES.containsKey(loc))
                .map(ForgeRegistries.ENTITY_TYPES::getValue)
                .collect(Collectors.toSet());

        // 缓存所有的集合数据，保证极速访问
        ironSpellbooksEntityIds = new java.util.HashSet<>(IRON_SPELLBOOKS_ENTITIES.get());
        accurateEntityIds = new java.util.HashSet<>(ACCURATE_ENTITIES.get());
        accurateLvl3EntityIds = new java.util.HashSet<>(ACCURATE_LVL3_ENTITIES.get());
        accurateLvl4EntityIds = new java.util.HashSet<>(ACCURATE_LVL4_ENTITIES.get());
        accurateLvl5EntityIds = new java.util.HashSet<>(ACCURATE_LVL5_ENTITIES.get());
        accurateLvl7EntityIds = new java.util.HashSet<>(ACCURATE_LVL7_ENTITIES.get());
        magicFocusEntityIds = new java.util.HashSet<>(MAGIC_FOCUS_ENTITIES.get());
        magicFocusLvl3EntityIds = new java.util.HashSet<>(MAGIC_FOCUS_LVL3_ENTITIES.get());
    }
}