package com.first.themasses.Client.sword;

import com.first.themasses.TheMasses;
import com.first.themasses.item.sword.VoidPiercer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID, value = Dist.CLIENT)
public class VoidPiercerRenderer {
    private static final Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent
    public static void onRenderHand(@NotNull RenderHandEvent event) {
        ItemStack itemStack = event.getItemStack();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // 适配判断：检测当前渲染的物品是否为 VioletSword
        if (itemStack.getItem() instanceof VoidPiercer) {
            // 检测玩家是否正在使用（右键格挡）
            if (player.isUsingItem() && player.getUseItem() == itemStack) {

                InteractionHand hand = event.getHand();
                HumanoidArm arm = (hand == InteractionHand.MAIN_HAND) ? player.getMainArm() : player.getMainArm().getOpposite();
                boolean isRightArm = arm == HumanoidArm.RIGHT;
                int side = isRightArm ? 1 : -1;

                PoseStack poseStack = event.getPoseStack();
                poseStack.pushPose();
                // --- 适配 1.7.10 风格参数 (修正版) ---
// 1. 位移修正：
// X: side * 0.4F  -> 将剑柄往屏幕边缘推一点，防止剑柄挡住中心
// Y: -0.2F        -> 降低高度，防止出现在左上角
// Z: -0.3F        -> 稍微拉远，确保能看清整把剑
                poseStack.translate(side * 0.24F, -0.35F, -0.4F);

                // 2. 核心旋转：
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(side * 25.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * 90.0F));

                // 3. 手动调用渲染器
                mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                        player,
                        itemStack,
                        isRightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                        !isRightArm, // 如果是左手，这里会自动处理镜像镜像
                        poseStack,
                        event.getMultiBufferSource(),
                        event.getPackedLight()
                );

                poseStack.popPose();

                // 取消原版默认渲染，防止重影
                event.setCanceled(true);
            }
        }
    }
}