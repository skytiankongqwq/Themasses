package com.first.themasses.Client.prop;

import com.first.themasses.util.ModAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// ... 其他代码 ...
@Mod.EventBusSubscriber(modid = "themasses", value = Dist.CLIENT)
public class MikuRenderHandler {

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        // 获取你自定义的属性值
        var instance = entity.getAttribute(ModAttributes.MIKU_SIZE.get());

        if (instance != null) {
            float scale = (float) instance.getValue(); // 这里拿到的就是计算后的 total 值
            if (scale != 1.0F) {
                event.getPoseStack().pushPose();
                event.getPoseStack().scale(scale, scale, scale);
                    // 这里利用 pushPose，在 Pre 事件里放大
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        var instance = event.getEntity().getAttribute(ModAttributes.MIKU_SIZE.get());
        if (instance != null && instance.getValue() != 1.0D) {
            event.getPoseStack().popPose(); // 渲染完后弹出来，防止影响其他渲染
        }
    }
}
