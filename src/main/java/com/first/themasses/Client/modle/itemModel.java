package com.first.themasses.Client.modle;

import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.BakedModelWrapper;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;
//物品栏和武器模型变化
public class itemModel extends BakedModelWrapper<BakedModel> {
    private final BakedModel guiModel;

    public itemModel(BakedModel originalModel, BakedModel guiModel) {
        super(originalModel);
        this.guiModel = guiModel;
    }

    @Override
    public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext displayContext, @NotNull PoseStack poseStack, boolean applyLeftHandTransform) {
        // 如果是物品栏 (GUI) 或 展示框 (FIXED)，切换到 GUI 模型
        if (displayContext == ItemDisplayContext.GUI || displayContext == ItemDisplayContext.FIXED) {
            return guiModel.applyTransform(displayContext, poseStack, applyLeftHandTransform);
        }
        // 否则（手里、地上），使用原本的 3D/高清模型逻辑
        return super.applyTransform(displayContext, poseStack, applyLeftHandTransform);
    }

    // --- 关键点：必须重写这个，否则渲染器可能无法正确识别模型切换 ---
    @Override
    public @NotNull ItemOverrides getOverrides() {
        return super.getOverrides();
    }
}