package com.first.themasses.Client.mysterious;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
//换皮逻辑
public class ClientHuanpiLogic {

    private static ResourceLocation originalSkin = null;

    public static void applyHuanpi(String name, String value, String sig) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.execute(() -> {
            // --- 1. 备份逻辑：必须在最顶端执行，防止被下方的 fallbackLoc 污染 ---
            if (originalSkin == null) {
                ResourceLocation current = getPlayerCurrentSkin();
                // 只有当当前皮肤不是我们 Mod 注入的皮肤，也不是占坑图时，才允许备份
                if (current != null && !current.getNamespace().equals("themasses") && !current.getPath().contains("fallback")) {
                    originalSkin = current;
                    System.out.println("!!! [Huanpi] 成功截获原始皮肤备份: " + originalSkin);
                }
            }

            // --- 2. 还原逻辑 ---
            if (name == null || name.isEmpty()) {
                System.out.println("!!! [Huanpi] 收到还原请求...");
                if (originalSkin != null) {
                    injectToMap(originalSkin);
                    System.out.println("!!! [Huanpi] 已通过备份还原皮肤: " + originalSkin);
                } else {
                    // 如果备份为空（比如进服就没换过皮肤却触发了还原），则尝试清空 Map 让系统自动重新加载
                    clearMap();
                    System.out.println("!!! [Huanpi] 备份缺失，尝试强制清空纹理映射以重置");
                }
                return;
            }

            // --- 3. 变身逻辑 ---
            System.out.println("!!! [Huanpi] 准备变身为: " + name);

            // 先变黑紫（占坑）
            ResourceLocation fallbackLoc = new ResourceLocation("minecraft", "textures/entity/player/" + name.toLowerCase() + "_fallback");
            injectToMap(fallbackLoc);

            if (value == null || value.isEmpty()) {
                System.out.println("!!! [Huanpi] 警告：数据为空，停止下载");
                return;
            }

            CompletableFuture.runAsync(() -> {
                try {
                    String jsonStr = new String(Base64.getDecoder().decode(value));
                    JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();

                    if (json.has("textures") && json.getAsJsonObject("textures").has("SKIN")) {
                        String skinUrl = json.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();

                        HttpURLConnection conn = (HttpURLConnection) new URL(skinUrl).openConnection();
                        conn.setConnectTimeout(5000);
                        try (InputStream is = conn.getInputStream()) {
                            NativeImage image = NativeImage.read(is);
                            mc.execute(() -> {
                                ResourceLocation dynamicLoc = new ResourceLocation("themasses", "skins/" + name.toLowerCase());
                                mc.getTextureManager().register(dynamicLoc, new DynamicTexture(image));
                                injectToMap(dynamicLoc);
                                System.out.println("!!! [Huanpi] 动态皮肤注入成功！路径: " + dynamicLoc);
                            });
                        }
                    }
                } catch (Exception e) {
                    System.err.println("!!! [Huanpi] 下载异常: " + e.getMessage());
                }
            });
        });
    }

    // 获取当前 Map 里的皮肤路径
    private static ResourceLocation getPlayerCurrentSkin() {
        Minecraft mc = Minecraft.getInstance();
        try {
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info != null) {
                Field field = getMapField();
                if (field != null) {
                    Map<MinecraftProfileTexture.Type, ResourceLocation> map = (Map) field.get(info);
                    return map.get(MinecraftProfileTexture.Type.SKIN);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private static void injectToMap(ResourceLocation loc) {
        Minecraft mc = Minecraft.getInstance();
        try {
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info != null && loc != null) {
                Field field = getMapField();
                if (field != null) {
                    Map<MinecraftProfileTexture.Type, ResourceLocation> map = (Map) field.get(info);
                    map.put(MinecraftProfileTexture.Type.SKIN, loc);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // 暴力清空 Map 的保底方法
    private static void clearMap() {
        Minecraft mc = Minecraft.getInstance();
        try {
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info != null) {
                Field field = getMapField();
                if (field != null) {
                    Map map = (Map) field.get(info);
                    if (map != null) map.clear();
                }
            }
        } catch (Exception e) { }
    }

    private static Field getMapField() {
        try {
            Field[] fields = PlayerInfo.class.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType().equals(Map.class)) {
                    field.setAccessible(true);
                    return field;
                }
            }
        } catch (Exception e) { }
        return null;
    }
}