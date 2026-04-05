package com.first.themasses.curios;

import com.google.common.collect.Multimap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;

/**
 * 魔法兼容逻辑封装类 - 完整版
 * 已严格区分 DaZhi 和 Haozi 的逻辑与 UUID
 */
public class IronsCompat {

    // --- DaZhi 专属 UUID ---
    private static final UUID DAZHI_SPELL_LEVEL_UUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
    private static final UUID DAZHI_MANA_UUID = UUID.fromString("a1b2c3d4-e5f6-4a5b-b6c7-d8e9f0a1b2c3");

    // --- Haozi 专属 UUID ---
    private static final UUID HAOZI_MANA_UUID = UUID.fromString("789abcde-1234-5678-90ab-cdef12345678");
    private static final UUID HAOZI_POWER_UUID = UUID.fromString("12345678-abcd-efab-1234-567890abcdef");
    private static final UUID HAOZI_COOLDOWN_UUID = UUID.fromString("87654321-dcba-feda-4321-fedcba098765");

    // ================= [ DAZHI 逻辑区 ] =================

    public static void imbueSpell(ItemStack stack) {
        if (stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.getBoolean("dazhi_spell_assigned")) {
            var container = io.redspace.ironsspellbooks.api.spells.ISpellContainer.create(1, true, true);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.SCULK_TENTACLES_SPELL.get();
            container.addSpell(spell, 1, true, stack);
            container.save(stack);
            tag.putBoolean("dazhi_spell_assigned", true);
        }
    }

    public static void onEquip(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.SCULK_TENTACLES_SPELL.get();
            io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(player).getSyncedData().learnSpell(spell);
        }
    }

    public static void addMagicAttributes(Multimap<Attribute, AttributeModifier> attributes) {
        Attribute maxMana = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", "max_mana"));
        if (maxMana != null) {
            attributes.put(maxMana, new AttributeModifier(DAZHI_MANA_UUID, "dazhi_mana", 100.0, AttributeModifier.Operation.ADDITION));
        }

        try {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.SCULK_TENTACLES_SPELL.get();
            String spellId = spell.getSpellId();
            if (spellId.contains(":")) {
                String cleanPath = spellId.split(":")[1];
                Attribute spellLevelAttr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", cleanPath + "_spell_level"));
                if (spellLevelAttr != null) {
                    attributes.put(spellLevelAttr, new AttributeModifier(DAZHI_SPELL_LEVEL_UUID, "dazhi_spell_level", 1.0, AttributeModifier.Operation.ADDITION));
                }
            }
        } catch (Exception ignored) {}
    }

    // ================= [ HAOZI 逻辑区 ] =================

    public static void imbueHaoziSpell(ItemStack stack) {
        if (stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();
        // 使用独立的 NBT Key
        if (!tag.getBoolean("haozi_spell_assigned")) {
            var container = io.redspace.ironsspellbooks.api.spells.ISpellContainer.create(1, true, true);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.CHAIN_LIGHTNING_SPELL.get();

            // 注入 15 级连锁闪电
            container.addSpell(spell, 15, true, stack);
            container.save(stack);
            tag.putBoolean("haozi_spell_assigned", true);
        }
    }

    public static void onHaoziEquip(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.CHAIN_LIGHTNING_SPELL.get();
            io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(player).getSyncedData().learnSpell(spell);
        }
    }

    public static void addHaoziMagicAttributes(Multimap<Attribute, AttributeModifier> attributes) {
        // 1. 最大法力 +200
        Attribute maxMana = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", "max_mana"));
        if (maxMana != null) {
            attributes.put(maxMana, new AttributeModifier(HAOZI_MANA_UUID, "haozi_mana", 200.0, AttributeModifier.Operation.ADDITION));
        }

        // 2. 法术强度 +25%
        Attribute spellPower = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", "spell_power"));
        if (spellPower != null) {
            attributes.put(spellPower, new AttributeModifier(HAOZI_POWER_UUID, "haozi_power", 0.25, AttributeModifier.Operation.MULTIPLY_BASE));
        }

        // 3. 法术冷却 -25%
        Attribute spellCooldown = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", "spell_cooldown"));
        if (spellCooldown != null) {
            attributes.put(spellCooldown, new AttributeModifier(HAOZI_COOLDOWN_UUID, "haozi_cooldown", -0.25, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }
}