package com.first.themasses.curios;

import com.google.common.collect.Multimap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;

/**
 * 魔法兼容逻辑封装类
 * 只有当 Irons Spellbooks 存在时，由主类逻辑通过 ModList 判断后才会触发对此类的访问。
 */
public class IronsCompat {

    private static final UUID SPELL_LEVEL_UUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
    private static final UUID MANA_UUID = UUID.fromString("a1b2c3d4-e5f6-4a5b-b6c7-d8e9f0a1b2c3");

    /**
     * 1. 注入法术
     */
    public static void imbueSpell(ItemStack stack) {
        if (stack.isEmpty()) return;

        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.getBoolean("spellAssigned")) {
            // 这里使用接口和注册表的 get()，如果没有 Mod 会在运行时崩溃，但因为外层有 ModList 检查，所以安全
            var container = io.redspace.ironsspellbooks.api.spells.ISpellContainer.create(1, true, true);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.SCULK_TENTACLES_SPELL.get();

            container.addSpell(spell, 1, true, stack);
            container.save(stack);

            tag.putBoolean("spellAssigned", true);
        }
    }

    /**
     * 2. 解锁法术权限
     */
    public static void onEquip(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            // 获取玩家魔法数据并解锁特定法术
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.SCULK_TENTACLES_SPELL.get();

            io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(player)
                    .getSyncedData()
                    .learnSpell(spell);
        }
    }

    /**
     * 3. 添加魔法相关属性
     */
    public static void addMagicAttributes(Multimap<Attribute, AttributeModifier> attributes) {
        // 使用 ForgeRegistries 动态获取，比直接引用 AttributeRegistry 静态字段更稳
        Attribute maxMana = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", "max_mana"));

        if (maxMana != null) {
            attributes.put(maxMana, new AttributeModifier(MANA_UUID, "dazhi_mana", 100.0, AttributeModifier.Operation.ADDITION));
        }

        try {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.SCULK_TENTACLES_SPELL.get();
            String spellId = spell.getSpellId();

            // 动态拼接法术等级属性 ID (例如 irons_spellbooks:sculk_tentacles -> sculk_tentacles_spell_level)
            if (spellId.contains(":")) {
                String cleanPath = spellId.split(":")[1];
                Attribute spellLevelAttr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", cleanPath + "_spell_level"));

                if (spellLevelAttr != null) {
                    attributes.put(spellLevelAttr, new AttributeModifier(SPELL_LEVEL_UUID, "dazhi_spell_level", 1.0, AttributeModifier.Operation.ADDITION));
                }
            }
        } catch (Exception ignored) {
            // 防止第三方 Mod 改 ID 或法术未注册导致崩溃
        }
    }
}