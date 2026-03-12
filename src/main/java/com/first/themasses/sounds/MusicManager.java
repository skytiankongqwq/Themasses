package com.first.themasses.sounds;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 音乐/音效管理器：负责控制客户端的声音播放逻辑
 */
public class MusicManager {
    // 记录每个玩家当前正在播放的音效实例（在客户端通常只存当前玩家自己）
    public static final Map<Player, SoundInstance> activeMusic = new HashMap<>();

    /**
     * 在客户端为玩家播放指定的音频
     * @param player 目标玩家
     * @param sound 注册的音频事件
     * @param loop 是否循环播放
     */
    @OnlyIn(Dist.CLIENT)
    public static void playMusicForPlayer(Player player, SoundEvent sound, Boolean loop) {
        if (player == null || sound == null) return;

        // 关键：只在本地玩家身上生效（避免听到其他人的 BGM 控制指令）
        if (player != Minecraft.getInstance().player) return;

        // 如果该玩家已经有正在播放的音效，先停止它，防止叠音
        if (activeMusic.containsKey(player)) {
            stopMusicForPlayer(player);
        }

        // 停止背景音乐，给技能音效腾出频道
        Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MUSIC);

        // 创建自定义音效实例 (这里使用的是你项目中的 CustomLoopingSound)
        // 注意：如果你只需要播放一次，可以直接用 SimpleSoundInstance
        CustomLoopingSound music = new CustomLoopingSound(
                player,
                sound,
                SoundSource.MUSIC,
                loop
        );

        // 执行播放
        Minecraft.getInstance().getSoundManager().play(music);

        // 存入 Map 以便后续通过代码停止
        activeMusic.put(player, music);
    }

    /**
     * 停止该玩家当前的所有自定义音效
     */
    @OnlyIn(Dist.CLIENT)
    public static void stopMusicForPlayer(Player player) {
        if (player == null || !activeMusic.containsKey(player)) return;

        SoundInstance music = activeMusic.get(player);
        if (music != null) {
            Minecraft.getInstance().getSoundManager().stop(music);
        }
        activeMusic.remove(player);
    }

    /**
     * 服务器调用接口：通过 ResourceLocation 查找并控制音效
     * 常用于 PacketHandle 类中
     */
    public static void handleMusicPacket(ResourceLocation musicResource, boolean play, Player player) {
        // DistExecutor 确保这段代码只在客户端运行，防止服务器崩溃
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(musicResource);
            if (sound != null) {
                if (play) {
                    playMusicForPlayer(player, sound, false); // 默认不循环，由逻辑端控制播放几次
                } else {
                    stopMusicForPlayer(player);
                }
            }
        });
    }
}