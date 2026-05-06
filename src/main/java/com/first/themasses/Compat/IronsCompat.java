package com.first.themasses.Compat;

import com.google.common.collect.Multimap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.SlotContext;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Supplier;
@SuppressWarnings("removal")
/**
 * 魔法兼容逻辑封装类 - 反射去符号版
 * 即使没有安装 Iron's Spellbooks，此类也不会导致崩溃
 */
public class IronsCompat {

    private static final UUID DAZHI_SPELL_LEVEL_UUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
    private static final UUID DAZHI_MANA_UUID = UUID.fromString("a1b2c3d4-e5f6-4a5b-b6c7-d8e9f0a1b2c3");
    private static final UUID HAOZI_MANA_UUID = UUID.fromString("789abcde-1234-5678-90ab-cdef12345678");
    private static final UUID HAOZI_POWER_UUID = UUID.fromString("12345678-abcd-efab-1234-567890abcdef");
    private static final UUID HAOZI_COOLDOWN_UUID = UUID.fromString("87654321-dcba-feda-4321-fedcba098765");

    // ================= [ 核心反射逻辑 ] =================

    private static boolean isIronsLoaded() {
        return ModList.get().isLoaded("irons_spellbooks");
    }

    /**
     * 获取 Spell 对象的通用方法
     * @param spellFieldName SpellRegistry 中的字段名，如 "SCULK_TENTACLES_SPELL"
     */
    private static Object getSpell(String spellFieldName) {
        try {
            Class<?> registryClass = Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry");
            Object supplier = registryClass.getField(spellFieldName).get(null);
            if (supplier instanceof Supplier<?> s) {
                return s.get();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    // ================= [ DAZHI 逻辑区 ] =================

    public static void imbueSpell(ItemStack stack) {
        if (!isIronsLoaded() || stack.isEmpty()) return;

        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.getBoolean("dazhi_spell_assigned")) {
            try {
                // 反射调用 ISpellContainer.create(1, true, true)
                Class<?> containerClass = Class.forName("io.redspace.ironsspellbooks.api.spells.ISpellContainer");
                Method createMethod = containerClass.getMethod("create", int.class, boolean.class, boolean.class);
                Object container = createMethod.invoke(null, 1, true, true);

                Object spell = getSpell("SCULK_TENTACLES_SPELL");
                if (spell != null) {
                    // 反射调用 container.addSpell(spell, level, locked, stack)
                    Class<?> spellClass = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
                    Method addSpellMethod = container.getClass().getMethod("addSpell", spellClass, int.class, boolean.class, ItemStack.class);
                    addSpellMethod.invoke(container, spell, 1, true, stack);

                    // 保存到物品
                    Method saveMethod = container.getClass().getMethod("save", ItemStack.class);
                    saveMethod.invoke(container, stack);

                    tag.putBoolean("dazhi_spell_assigned", true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void onEquip(SlotContext slotContext, ItemStack stack) {
        if (!isIronsLoaded()) return;
        if (slotContext.entity() instanceof Player player) {
            try {
                Object spell = getSpell("SCULK_TENTACLES_SPELL");
                if (spell != null) {
                    // MagicData.getPlayerMagicData(player).getSyncedData().learnSpell(spell)
                    Class<?> magicDataClass = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
                    Method getMethod = magicDataClass.getMethod("getPlayerMagicData", Player.class);
                    Object magicData = getMethod.invoke(null, player);

                    Method getSyncedDataMethod = magicData.getClass().getMethod("getSyncedData");
                    Object syncedData = getSyncedDataMethod.invoke(magicData);

                    Class<?> spellClass = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
                    Method learnMethod = syncedData.getClass().getMethod("learnSpell", spellClass);
                    learnMethod.invoke(syncedData, spell);
                }
            } catch (Exception ignored) {}
        }
    }

    public static void addMagicAttributes(Multimap<Attribute, AttributeModifier> attributes) {
        if (!isIronsLoaded()) return;

        Attribute maxMana = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", "max_mana"));
        if (maxMana != null) {
            attributes.put(maxMana, new AttributeModifier(DAZHI_MANA_UUID, "dazhi_mana", 100.0, AttributeModifier.Operation.ADDITION));
        }

        try {
            Object spell = getSpell("SCULK_TENTACLES_SPELL");
            if (spell != null) {
                Method getSpellIdMethod = spell.getClass().getMethod("getSpellId");
                String spellId = (String) getSpellIdMethod.invoke(spell);

                if (spellId.contains(":")) {
                    String cleanPath = spellId.split(":")[1];
                    Attribute spellLevelAttr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", cleanPath + "_spell_level"));
                    if (spellLevelAttr != null) {
                        attributes.put(spellLevelAttr, new AttributeModifier(DAZHI_SPELL_LEVEL_UUID, "dazhi_spell_level", 1.0, AttributeModifier.Operation.ADDITION));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    // ================= [ HAOZI 逻辑区 ] =================

    public static void imbueHaoziSpell(ItemStack stack) {
        if (!isIronsLoaded() || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.getBoolean("haozi_spell_assigned")) {
            try {
                Class<?> containerClass = Class.forName("io.redspace.ironsspellbooks.api.spells.ISpellContainer");
                Method createMethod = containerClass.getMethod("create", int.class, boolean.class, boolean.class);
                Object container = createMethod.invoke(null, 1, true, true);

                Object spell = getSpell("CHAIN_LIGHTNING_SPELL");
                if (spell != null) {
                    Class<?> spellClass = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
                    Method addSpellMethod = container.getClass().getMethod("addSpell", spellClass, int.class, boolean.class, ItemStack.class);
                    addSpellMethod.invoke(container, spell, 15, true, stack);

                    Method saveMethod = container.getClass().getMethod("save", ItemStack.class);
                    saveMethod.invoke(container, stack);
                    tag.putBoolean("haozi_spell_assigned", true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void onHaoziEquip(SlotContext slotContext, ItemStack stack) {
        if (!isIronsLoaded()) return;
        if (slotContext.entity() instanceof Player player) {
            try {
                Object spell = getSpell("CHAIN_LIGHTNING_SPELL");
                if (spell != null) {
                    Class<?> magicDataClass = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
                    Method getMethod = magicDataClass.getMethod("getPlayerMagicData", Player.class);
                    Object magicData = getMethod.invoke(null, player);

                    Method getSyncedDataMethod = magicData.getClass().getMethod("getSyncedData");
                    Object syncedData = getSyncedDataMethod.invoke(magicData);

                    Class<?> spellClass = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
                    Method learnMethod = syncedData.getClass().getMethod("learnSpell", spellClass);
                    learnMethod.invoke(syncedData, spell);
                }
            } catch (Exception ignored) {}
        }
    }

    public static void addHaoziMagicAttributes(Multimap<Attribute, AttributeModifier> attributes) {
        if (!isIronsLoaded()) return;

        Attribute maxMana = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", "max_mana"));
        if (maxMana != null) {
            attributes.put(maxMana, new AttributeModifier(HAOZI_MANA_UUID, "haozi_mana", 200.0, AttributeModifier.Operation.ADDITION));
        }

        Attribute spellPower = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", "spell_power"));
        if (spellPower != null) {
            attributes.put(spellPower, new AttributeModifier(HAOZI_POWER_UUID, "haozi_power", 0.25, AttributeModifier.Operation.MULTIPLY_BASE));
        }

        Attribute spellCooldown = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", "spell_cooldown"));
        if (spellCooldown != null) {
            attributes.put(spellCooldown, new AttributeModifier(HAOZI_COOLDOWN_UUID, "haozi_cooldown", -0.25, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }
}