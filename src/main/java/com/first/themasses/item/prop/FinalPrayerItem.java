package com.first.themasses.item.prop;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
//普通祈福 51%---10%
//「千斩」 - 攻击时有概率追加一次额外斩击
//「连斩」 - 10s内，击杀目标的话攻击速度翻倍1分钟
//「破甲」 - 无视目标30%防御，持续5秒
//「水行」 - 水下移动速度+50%，持续2分钟
//「精准」 - 接下来3次攻击+50%暴击率
//「夜视」 - 获得夜视效果，持续3分钟
//「迅捷」 - 移动速度+30%，持续40秒
//强化祈福 30%---10%
//「嗜血」 - 击杀敌人恢复最大生命值5% 持续10s
//「金刚」 - 免疫击退和硬直，持续15秒
//「破军」 - 攻击对周围3格内所有敌人造成溅射伤害 持续10s
//「无双」 - 单挑时（周围无队友）伤害+60%，持续20秒
//「隐身伏击」 - 进入隐身状态5秒，破隐一击造成200%伤害
//神话祈福 13%---60%
//「洁净之躯」 - 60秒内免疫所有负面状态
//「死亡宣告」 - 下一刀会标记敌人，30秒内对其伤害+500%，但期间不能攻击其他目标
//「绝对防御」 - 5秒内免疫所有伤害，但无法移动和攻击
//终之祈 1%---20%
//一个几乎耗尽一切的攻击
//扭曲祈福 3% （持续时间均无限，直到下一次使用祈福或者死亡）
//「血肉流失」   正效：攻击速度+100%，吸血+30%   负效：每秒流失3%最大生命值，每10s增加1%，每次攻击额外扣除2点生命
//「扭曲空间」   正效：攻击距离*10倍，可命中视线内任何目标    负效：自身位置随机波动（每秒可能偏移1-3格）
//「被诅咒者」   正效：伤害增加*10（独立加成），负效：收到的所有伤害变成kill伤害
//诅咒祈福 2% （直到死亡）
//「生命献祭」效果：每秒燃烧最大生命值的2%，持续到死亡
//「脆弱之躯」效果：收到的所有伤害会叠加到下一次伤害（收到三次初始为1点的伤害会变成1.2.3，可以持续叠加）
//「刃之回响」效果：造成伤害的120%反弹自身
public class FinalPrayerItem extends Item {
    public FinalPrayerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack pStack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        // --- 冷却检查 ---
        if (pPlayer.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(pPlayer.getItemInHand(pHand));
        }

        pPlayer.startUsingItem(pHand);
        return InteractionResultHolder.consume(pPlayer.getItemInHand(pHand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (entity instanceof Player player && !level.isClientSide) {
            int duration = this.getUseDuration(stack) - count;
            if (duration > 0 && duration % 20 == 0) {
                player.hurt(level.damageSources().magic(), 1.0f);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player && !level.isClientSide) {
            int chargeSeconds = (this.getUseDuration(stack) - timeLeft) / 20;
            chargeSeconds = Math.min(chargeSeconds, 10);

            // 调用概率引擎
            BlessingEngine.applyBlessing(player, chargeSeconds);

            // --- 新增：设置 60s (1200 ticks) 冷却 ---
            player.getCooldowns().addCooldown(this, 1200);
        }
    }

    // --- 新增：物品描述 ---
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.themasses.final_prayer.line1").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x8B0000))));
    }
}