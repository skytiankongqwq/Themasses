package com.first.themasses.curios.ring;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.List;
import java.util.UUID;
//妙用之戒
public class heishatangring extends Item {
    private static final UUID REACH_UUID = UUID.fromString("780c1097-3932-4735-9c86-905c754d7e91");
    private static final UUID SPEED_UUID = UUID.fromString("a10d2108-4043-5846-ad97-016d865e8f92");
    private static final UUID DAMAGE_UUID = UUID.fromString("b21e3219-5154-6957-be08-127e976f90a3");
    private static final UUID STEP_UUID = UUID.fromString("e10d2108-4043-5846-ad97-016d865e8f92");

    public heishatangring(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void toggleMode(Player player) {
        CompoundTag nbt = player.getPersistentData();
        int mode = (nbt.getInt("RingMode") + 1) % 4;
        nbt.putInt("RingMode", mode);

        String[] names = {"常规", "长臂猿 ", "闪电侠 ", "大力士 "};
        ChatFormatting[] colors = {ChatFormatting.GRAY, ChatFormatting.GOLD, ChatFormatting.AQUA, ChatFormatting.RED};

        player.displayClientMessage(Component.literal("调律状态 -> " + names[mode]).withStyle(colors[mode]), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 0.5f + (mode * 0.2f));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                CompoundTag nbt = stack.getOrCreateTag();
                int magnet = (nbt.getInt("MagnetMode") + 1) % 3;
                nbt.putInt("MagnetMode", magnet);
                String name = magnet == 0 ? "关闭" : (magnet == 1 ? "吸附" : "排斥");
                player.displayClientMessage(Component.literal("生物磁场: " + name).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final LazyOptional<ICurio> curio = LazyOptional.of(() -> new ICurio() {

                @Override
                public void curioTick(SlotContext slotContext) {
                    LivingEntity entity = slotContext.entity();
                    if (!(entity instanceof Player player)) return;

                    CompoundTag playerNbt = player.getPersistentData();
                    int mode = playerNbt.getInt("RingMode");

                    // 1. 属性更新 (包含5格登高修复)
                    if (!player.level().isClientSide) {
                        updateAttributes(player, mode);
                    }

                    // 2. 磁场逻辑 (必须在服务端运行)
                    if (!player.level().isClientSide) {
                        CompoundTag itemNbt = stack.getOrCreateTag();
                        int magnet = itemNbt.getInt("MagnetMode");

                        if (magnet > 0) {
                            double targetRange = 2.0;
                            // 核心改动：搜索所有 Entity，排除掉落物 (ItemEntity) 和 玩家自己
                            List<Entity> targets = player.level().getEntities(player,
                                    player.getBoundingBox().inflate(8.0),
                                    e -> !(e instanceof net.minecraft.world.entity.item.ItemEntity) && e.isAlive());

                            for (Entity target : targets) {
                                Vec3 targetPos = target.position();
                                Vec3 playerPos = player.position();
                                double dist = playerPos.distanceTo(targetPos);

                                // 计算从目标指向玩家的向量 (吸附向量)
                                Vec3 motionVec = playerPos.subtract(targetPos).normalize();

                                if (magnet == 1) { // --- 吸附模式 ---
                                    if (dist > targetRange + 0.5) {
                                        // 增加吸力
                                        target.setDeltaMovement(target.getDeltaMovement().add(motionVec.scale(0.18)));
                                    } else if (dist < targetRange - 0.2) {
                                        // 太近了，轻微推开
                                        target.setDeltaMovement(target.getDeltaMovement().add(motionVec.scale(-0.12)));
                                    } else {
                                        // 进入 2 格平衡区，减缓其原有动能
                                        target.setDeltaMovement(target.getDeltaMovement().multiply(0.6, 1, 0.6));
                                    }
                                } else { // --- 排斥模式 ---
                                    // 增强排斥力，确保能弹开高速飞行的箭矢
                                    target.setDeltaMovement(target.getDeltaMovement().add(motionVec.scale(-0.35)));
                                }

                                // 关键：非生物实体（如箭矢）必须设置这个，否则客户端位置不会更新
                                target.hurtMarked = true;

                                // 特殊处理：如果是弹射物（箭矢、火球等），修改其朝向以匹配新速度
                                if (target instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
                                    Vec3 velocity = target.getDeltaMovement();
                                    target.setYRot((float)(Math.atan2(velocity.x, velocity.z) * (180D / Math.PI)));
                                    target.setXRot((float)(Math.atan2(velocity.y, velocity.horizontalDistance()) * (180D / Math.PI)));
                                }
                            }
                        }
                    }

                    // 3. 反重力逻辑 (两端运行)
                    if (player.isShiftKeyDown() && !player.onGround() && player.getDeltaMovement().y < 0) {
                        player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0.6, 1));
                        player.fallDistance = 0;
                    }
                }

                @Override
                public void onUnequip(SlotContext slotContext, ItemStack newStack) {
                    if (slotContext.entity() instanceof Player player && !player.level().isClientSide) {
                        updateAttributes(player, 0);
                    }
                }

                @Override
                public ItemStack getStack() {
                    return stack;
                }
            });

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                if (cap == CuriosCapability.ITEM) {
                    return curio.cast();
                }
                return LazyOptional.empty();
            }
        };
    }

    private void updateAttributes(Player player, int mode) {
        modifyAttr(player, ForgeMod.BLOCK_REACH.get(), REACH_UUID, "ReachMod", (mode == 1 ? 10.0 : 0.0));
        modifyAttr(player, ForgeMod.ENTITY_REACH.get(), REACH_UUID, "AtkReachMod", (mode == 1 ? 10.0 : 0.0));
        modifyAttr(player, Attributes.MOVEMENT_SPEED, SPEED_UUID, "SpeedMod", (mode == 2 ? 0.4 : 0.0));
        modifyAttr(player, Attributes.ATTACK_DAMAGE, DAMAGE_UUID, "DmgMod", (mode == 3 ? 10.0 : 0.0));
        modifyAttr(player, Attributes.ATTACK_KNOCKBACK, DAMAGE_UUID, "KbkMod", (mode == 3 ? 50.0 : 0.0));

        // 登高高度修复：使用 ForgeMod.STEP_HEIGHT_ADDITION 属性，这是 1.20.1 最稳定的写法
        modifyAttr(player, ForgeMod.STEP_HEIGHT_ADDITION.get(), STEP_UUID, "StepMod", (mode == 2 ? 4.4 : 0.0));
    }

    private void modifyAttr(Player player, net.minecraft.world.entity.ai.attributes.Attribute attr, UUID uuid, String name, double value) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst != null) {
            inst.removeModifier(uuid);
            if (value != 0) {
                inst.addTransientModifier(new AttributeModifier(uuid, name, value, AttributeModifier.Operation.ADDITION));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("▶ 空中 [潜行] 触发反重力漂浮").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("▶ [Shift+右键] 切换生物磁场").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("▶ [Shift+R] 切换模式").withStyle(ChatFormatting.YELLOW));
    }
}