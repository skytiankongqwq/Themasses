package com.first.themasses.sounds;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CustomLoopingSound extends AbstractTickableSoundInstance {
    private final Player player;

    public CustomLoopingSound(Player player, SoundEvent soundEvent, SoundSource source, boolean loop) {
        super(soundEvent, source, player.level().getRandom());
        this.player = player;
        this.looping = loop; // 设置是否循环
        this.delay = 0;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        // 声音跟随玩家位置
        this.x = (double)((float)player.getX());
        this.y = (double)((float)player.getY());
        this.z = (double)((float)player.getZ());
    }

    @Override
    public void tick() {
        // 如果玩家死亡或移除，停止播放
        if (!this.player.isAlive()) {
            this.stop();
        } else {
            // 实时更新声音位置，确保声音跟着人走
            this.x = (double)((float)this.player.getX());
            this.y = (double)((float)this.player.getY());
            this.z = (double)((float)this.player.getZ());
        }
    }
}