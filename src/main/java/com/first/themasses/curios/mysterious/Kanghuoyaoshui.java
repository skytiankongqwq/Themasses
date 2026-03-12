package com.first.themasses.curios.mysterious;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ForgeMod;

import java.util.List;
import java.util.Random;
import java.util.UUID;
//抗火药水的饰品类
@Mod.EventBusSubscriber(modid = "themasses")
public class Kanghuoyaoshui extends Item implements ICurio {

    public static final UUID FIRE_IMMUNITY_UUID = UUID.fromString("6a4f913e-2b1b-43d9-9524-78082989c743");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("d1e2f3a4-b5c6-7d8e-9f0a-1b2c3d4e5f6a");

    private static final double FIRE_IMMUNITY_MARKER = 0.0;
    private static final Random RANDOM = new Random();

    public Kanghuoyaoshui(Properties properties) {
        super(properties);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
            handler.findFirstCurio(stack -> stack.getItem() instanceof Kanghuoyaoshui).ifPresent(slotResult -> {
                ItemStack stack = slotResult.stack();
                CompoundTag tag = stack.getOrCreateTag();
                if (entity.getHealth() >= entity.getMaxHealth()) {
                    event.setAmount(event.getAmount() * 0.05F);
                }
                int layers = tag.getInt("SprintDamageLayers");
                if (layers > 0) {
                    event.setAmount(event.getAmount() * (1.0F + layers * 0.005F));
                }
            });
        });
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && !attacker.level().isClientSide) {
            CuriosApi.getCuriosInventory(attacker).ifPresent(handler -> {
                handler.findFirstCurio(stack -> stack.getItem() instanceof Kanghuoyaoshui).ifPresent(slotResult -> {
                    ItemStack itemStack = slotResult.stack();
                    CompoundTag tag = itemStack.getOrCreateTag();

                    if (tag.getBoolean("SpeedLocked")) return;

                    int currentCount = tag.getInt("AttackCount") + 1;
                    tag.putInt("AttackCount", currentCount);

                    double finalSpeedVal;

                    if (currentCount >= 100) {
                        tag.putInt("AttackCount", 0);
                        finalSpeedVal = (RANDOM.nextFloat() < 0.8F) ? 1.0 : -1.0;
                        tag.putBoolean("SpeedLocked", true);
                        tag.putLong("LockEndTime", attacker.level().getGameTime() + 200);
                    } else {
                        finalSpeedVal = Math.round((-1.0 + RANDOM.nextDouble() * 2.0) * 1000.0) / 1000.0;
                        if (Math.abs(finalSpeedVal) >= 1.0) {
                            tag.putBoolean("SpeedLocked", true);
                            tag.putLong("LockEndTime", attacker.level().getGameTime() + 200);
                        }
                    }

                    AttributeInstance inst = attacker.getAttribute(Attributes.ATTACK_SPEED);
                    if (inst != null) {
                        inst.removeModifier(ATTACK_SPEED_UUID);
                        inst.addTransientModifier(new AttributeModifier(ATTACK_SPEED_UUID, "Kanghuo Speed Mod", finalSpeedVal, AttributeModifier.Operation.ADDITION));
                    }
                });
            });
        }
    }

    @Override
    public void curioTick(SlotContext slotContext) {
        LivingEntity entity = slotContext.entity();
        if (entity.level().isClientSide) return;

        CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
            handler.findFirstCurio(s -> s.getItem() instanceof Kanghuoyaoshui).ifPresent(result -> {
                ItemStack stack = result.stack();
                CompoundTag tag = stack.getOrCreateTag();
                long gameTime = entity.level().getGameTime();

                BuiltInRegistries.MOB_EFFECT.stream()
                        .filter(effect -> BuiltInRegistries.MOB_EFFECT.getKey(effect).getPath().equals("bamboozled"))
                        .findFirst()
                        .ifPresent(effect -> {
                            if (entity.hasEffect(effect)) entity.removeEffect(effect);
                        });

                if (entity.isSprinting()) {
                    int sprintTicks = tag.getInt("SprintTicks") + 1;
                    tag.putInt("SprintTicks", sprintTicks);
                    if (sprintTicks % 200 == 0) {
                        tag.putInt("SprintDamageLayers", tag.getInt("SprintDamageLayers") + 1);
                    }
                    if (sprintTicks >= 1200 && entity instanceof Player p) {
                        if (!p.getAbilities().mayfly) {
                            p.getAbilities().mayfly = true;
                            p.onUpdateAbilities();
                        }
                    }
                } else if (tag.getInt("SprintTicks") > 0) {
                    tag.putInt("SprintTicks", 0);
                    tag.putInt("SprintDamageLayers", 0);
                    if (entity instanceof Player p && !p.isCreative() && !p.isSpectator()) {
                        p.getAbilities().mayfly = false;
                        p.getAbilities().flying = false;
                        p.onUpdateAbilities();
                    }
                }

                if (entity.getHealth() < entity.getMaxHealth() && gameTime % 80 == 0) {
                    entity.hurt(entity.damageSources().magic(), 3.0F + RANDOM.nextFloat() * 3.0F);
                }

                if (tag.getBoolean("SpeedLocked") && gameTime >= tag.getLong("LockEndTime")) {
                    tag.putBoolean("SpeedLocked", false);
                    AttributeInstance inst = entity.getAttribute(Attributes.ATTACK_SPEED);
                    if (inst != null) inst.removeModifier(ATTACK_SPEED_UUID);
                }

                if (gameTime % 5 == 0 && entity.isOnFire()) {
                    entity.clearFire();
                }
            });
        });
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        final LazyOptional<ICurio> lazyCurio = LazyOptional.of(() -> this);
        return new ICapabilityProvider() {
            @Override
            @SuppressWarnings("unchecked")
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                if (cap == CuriosCapability.ITEM) return (LazyOptional<T>) (Object) lazyCurio;
                return LazyOptional.empty();
            }
        };
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid) {
        Multimap<Attribute, AttributeModifier> attributes = HashMultimap.create();
        attributes.put(ForgeMod.SWIM_SPEED.get(), new AttributeModifier(FIRE_IMMUNITY_UUID, "Curio Fire Immunity Marker", FIRE_IMMUNITY_MARKER, AttributeModifier.Operation.ADDITION));
        return attributes;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // 仅保留你的自定义翻译文本
        tooltip.add(Component.translatable("tooltip.themasses.green_hand_PoTF.line1")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00AAAA))));
    }

    // 必须重写此方法，否则 Curios 会在下方自动添加槽位说明
    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltips) {
        return tooltips; // 直接返回，不让 Curios 往里加字
    }

    @Override public ItemStack getStack() { return ItemStack.EMPTY; }
    @Override public boolean canEquip(SlotContext slotContext) { return true; }
    @Override public boolean canUnequip(SlotContext slotContext) { return true; }
    @Override public void onEquip(SlotContext slotContext, ItemStack prevStack) { }
    @Override public void onUnequip(SlotContext slotContext, ItemStack newStack) { }
}