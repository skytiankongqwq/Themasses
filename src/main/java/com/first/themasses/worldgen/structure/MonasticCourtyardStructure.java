package com.first.themasses.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class MonasticCourtyardStructure extends Structure {
    public static final Codec<MonasticCourtyardStructure> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(settingsCodec(instance),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                    ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
                    Codec.intRange(0, 30).fieldOf("size").forGetter(s -> s.size)
            ).apply(instance, MonasticCourtyardStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int size;

    public MonasticCourtyardStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, Optional<ResourceLocation> startJigsawName, int size) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.size = size;
    }

    @Override
    public @NotNull Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();

        // 1. 强制锁定在水平面（海平面 64，或者根据你的需求微调）
        // 如果你的结构包含地基，可以设为 63；如果是浮在水面，设为 64。
        int seaLevel = 59;
        BlockPos spawnPos = new BlockPos(chunkPos.getMinBlockX(), seaLevel, chunkPos.getMinBlockZ());

        // 2. 直接调用生成
        return JigsawPlacement.addPieces(
                context,
                this.startPool,
                this.startJigsawName,
                this.size,
                spawnPos,
                false,
                Optional.empty(),
                128
        );
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.MONASTIC_COURTYARD.get();
    }
}