package com.first.themasses.Client.modle;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.BakedModelWrapper;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;
//万魂幡的材质
public class itemModel extends BakedModelWrapper<BakedModel> {
    private final BakedModel guiModel;

    public itemModel(BakedModel originalModel, BakedModel guiModel) {
        super(originalModel);
        this.guiModel = guiModel;
    }

    @Override
    public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext displayContext, @NotNull PoseStack poseStack, boolean applyLeftHandTransform) {
        // 如果是物品栏或展示框，切换到 2D 贴图模型的变换逻辑
        if (displayContext == ItemDisplayContext.GUI || displayContext == ItemDisplayContext.FIXED) {
            return guiModel.applyTransform(displayContext, poseStack, applyLeftHandTransform);
        }
        // 否则（手里、地上），使用原本的 3D 模型逻辑
        return super.applyTransform(displayContext, poseStack, applyLeftHandTransform);
    }
}