package com.first.themasses.Client.mysterious;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//换皮的渲染逻辑
@SuppressWarnings("removal")
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientRenderHandler {

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();

        // 1. 检查是否有变身数据
        String huanpiName = player.getPersistentData().getString("HuanpiName");

        if (huanpiName != null && !huanpiName.isEmpty()) {
            // 2. 动态获取皮肤路径
            // 这里我们利用 Minecraft 内部机制：根据名字计算一个“伪造”的皮肤路径
            // 离线模式下，Minecraft 会尝试通过这个路径去本地缓存或皮肤服务器找
            ResourceLocation skinLocation = getDynamicSkin(huanpiName);

            // 注意：这种方式在开发端最稳妥的做法是配合 SkinManager
            // 但如果只是为了测试效果，我们可以尝试让渲染器直接“认领”这个名字对应的皮肤
        }
    }

    private static ResourceLocation getDynamicSkin(String name) {
        // 返回该玩家名对应的皮肤位置（这是 Minecraft 默认的资源路径规则）
        return new ResourceLocation("skins/" + name.toLowerCase());
    }
}