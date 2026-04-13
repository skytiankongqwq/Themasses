package com.first.themasses.command.title;

import com.first.themasses.curios.mysterious.Duvacein;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.*;

//头衔和换皮的代码
public class ZhilingCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("touxian")
                .requires(source -> source.hasPermission(4))

                .then(Commands.literal("clear")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            player.getPersistentData().remove("CustomTitle");
                            player.getPersistentData().remove("CustomTitleColor");
                            player.refreshDisplayName();
                            context.getSource().sendSuccess(() -> Component.literal("已成功清除头衔数据"), true);
                            return 1;
                        }))

                // 头衔
                .then(Commands.argument("text", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            builder.suggest("\"\"", Component.literal("在引号内输入中文头衔"));
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            // 默认蓝色
                            return saveTitle(context.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(context, "text"), "blue", context.getSource());
                        })
                        .then(Commands.argument("color", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    builder.suggest("white"); builder.suggest("red");
                                    builder.suggest("blue"); builder.suggest("gold");
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    return saveTitle(context.getSource().getPlayerOrException(),
                                            StringArgumentType.getString(context, "text"),
                                            StringArgumentType.getString(context, "color"), context.getSource());
                                })
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> {
                                            // 获取指定的目标玩家
                                            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
                                            return saveTitle(targetPlayer,
                                                    StringArgumentType.getString(context, "text"),
                                                    StringArgumentType.getString(context, "color"), context.getSource());
                                        }))))
        );
        //duvacein饰品指令换皮
        dispatcher.register(Commands.literal("huanpi")
                .then(Commands.argument("id", StringArgumentType.string())
                        .executes(context -> performHuanpi(context.getSource(), StringArgumentType.getString(context, "id"))))
                .executes(context -> performRestore(context.getSource())));
    }
    //头衔逻辑
    private static int saveTitle(ServerPlayer player, String text, String color, CommandSourceStack source) {
        // 这里的 text 会自动处理引号，我们只需要正常校验长度
        if (text.length() > 12) {
            source.sendFailure(Component.literal("设置失败：头衔内容过长！"));
            return 0;
        }

        if (TextColor.parseColor(color) == null) {
            source.sendFailure(Component.literal("无效的颜色格式！"));
            return 0;
        }

        player.getPersistentData().putString("CustomTitle", "[" + text + "]");
        player.getPersistentData().putString("CustomTitleColor", color.toLowerCase());
        player.refreshDisplayName();

        // 解决 effectively final 问题
        final String fText = text;
        final String fColor = color;
        source.sendSuccess(() -> Component.literal("头衔设置成功！头衔: [" + fText + "] 颜色: " + fColor), true);
        return 1;
    }
    //换皮逻辑
    private static int performHuanpi(CommandSourceStack source, String targetName) {
        try {
            ServerPlayer player = source.getPlayerOrException();

            if (CuriosApi.getCuriosHelper().findFirstCurio(player, s -> s.getItem() instanceof Duvacein).isEmpty()) {
                source.sendFailure(Component.literal("§c你没有佩戴对应的饰品！"));
                return 0;
            }

            // 1. 存入 NBT
            player.getPersistentData().putString("HuanpiName", targetName);

            String textureValue = "";
            String textureSig = "";

            try {
                // 2. 在 1.20.1 中，直接 new GameProfile(null, name) 有时无法抓取
                // 最好先通过 ProfileCache 获取真实的 UUID
                var profileOpt = player.getServer().getProfileCache().get(targetName);
                GameProfile targetProfile;

                if (profileOpt.isPresent()) {
                    targetProfile = profileOpt.get();
                } else {
                    // 如果缓存没有，强行创建一个（注意：离线 UUID 可能抓不到正版皮肤）
                    targetProfile = new GameProfile(null, targetName);
                }

                // 3. 核心抓取：从 Mojang 填充属性
                // 第二个参数 true 表示请求签名数据（必须为 true 客户端才能加载）
                targetProfile = player.getServer().getSessionService().fillProfileProperties(targetProfile, true);

                if (targetProfile != null && !targetProfile.getProperties().get("textures").isEmpty()) {
                    com.mojang.authlib.properties.Property textures = targetProfile.getProperties().get("textures").iterator().next();

                    // 修改服务端自身的渲染属性（让其他玩家能看到）
                    player.getGameProfile().getProperties().removeAll("textures");
                    player.getGameProfile().getProperties().put("textures", textures);

                    textureValue = textures.getValue();
                    textureSig = textures.getSignature();
                    System.out.println("DEBUG: 皮肤数据抓取成功 - 长度: " + textureValue.length());
                } else {
                    System.out.println("DEBUG: Mojang 未返回皮肤数据，请检查网络或 ID 是否存在！");
                }
            } catch (Exception skinEx) {
                System.err.println("DEBUG: 皮肤抓取异常: " + skinEx.getMessage());
            }

            // 4. 刷新服务端显示
            refresh(player);

            // 5. --- 同步给客户端 ---
            // 关键：即使 textureValue 是空的，包也要发出去，客户端才知道要改 ID
            com.first.themasses.networking.ModMessages.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new com.first.themasses.networking.PacketSyncHuanpi(targetName, textureValue, textureSig)
            );

            source.sendSuccess(() -> Component.literal("§a 变身同步已发送: " + targetName), true);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    private static int performRestore(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();

            // 1. 服务端数据清理：移除变身标记
            player.getPersistentData().remove("HuanpiName");

            // 2. 核心修复：还原服务端的原始皮肤属性
            // 先移除变身的纹理
            player.getGameProfile().getProperties().removeAll("textures");
            try {
                // 从 Mojang 重新获取该玩家原始的皮肤属性并填入
                // 这确保了服务器知道你“本来的样子”，从而正确同步给其他玩家
                player.getServer().getSessionService().fillProfileProperties(player.getGameProfile(), true);
            } catch (Exception skinEx) {
                System.err.println("DEBUG: 还原原始皮肤属性时失败: " + skinEx.getMessage());
            }

            // 3. 通知客户端：执行本地备份还原
            // 名字传 "" 会触发我们 ClientHuanpiLogic 里的 originalSkin 还原
            com.first.themasses.networking.ModMessages.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new com.first.themasses.networking.PacketSyncHuanpi("", "", "")
            );

            // 4. 刷新视觉：发送 Packet 告知周围玩家更新你的 Profile
            refresh(player);

            source.sendSuccess(() -> Component.literal("§e 已重置状态。"), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    public static void refresh(ServerPlayer player) {
        if (player.getServer() == null) return;

        // 1. 先发 REMOVE_PLAYER 移除老数据
        player.getServer().getPlayerList().broadcastAll(
                new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED), List.of(player))
        );

        // 2. 发送完整的更新包（包含属性更新）
        // 注意：1.20.1 需要包含这些 Action 才能让客户端重新读取 GameProfile 里的纹理
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                EnumSet.allOf(ClientboundPlayerInfoUpdatePacket.Action.class), // 暴力刷新所有属性
                List.of(player)
        );
        player.getServer().getPlayerList().broadcastAll(packet);

        // 3. 强制触发客户端对该实体的重新载入（这解决“站着不动皮肤不变”的问题）
        // 让周围所有玩家（包括自己）重新看到自己
        player.getServer().getLevel(player.level().dimension()).getChunkSource().broadcastAndSend(player,
                new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(player.getId(), player.getEntityData().getNonDefaultValues())
        );

        player.refreshDisplayName();
    }
}