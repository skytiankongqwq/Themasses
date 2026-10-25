package com.first.themasses.events;

import com.first.themasses.TheMasses;
import com.first.themasses.attributes.ScaleAttributeHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按体型属性缩放实体渲染。从 Dream Sakura 的 RenderLivingEventHandler 移植，
 * 去掉了玩家梯子相关的偏移逻辑。
 */
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class RenderScaleHandler {

    /**
     * 记录当前帧哪些实体 push 过 pose，保证 Post 一定能配对 pop。
     */
    private static final Set<Integer> PUSHED_ENTITIES = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        double scale = ScaleAttributeHelper.getScale(living);
        if (ScaleAttributeHelper.isDefault(scale)) {
            return;
        }

        float scaleF = (float) scale;
        event.getPoseStack().pushPose();
        PUSHED_ENTITIES.add(entity.getId());
        event.getPoseStack().scale(scaleF, scaleF, scaleF);
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (PUSHED_ENTITIES.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }
}
