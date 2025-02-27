package io.redspace.ironsspellbooks.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.item.curios.RingData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class AffinityRingRenderer extends BlockEntityWithoutLevelRenderer {


    private final ItemRenderer renderer;
    private final ResourceLocation defaultModel = IronsSpellbooks.id("item/affinity_ring_evocation");

    public AffinityRingRenderer(ItemRenderer renderDispatcher, EntityModelSet modelSet) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), modelSet);
        this.renderer = renderDispatcher;
    }

    @Override
    public void renderByItem(ItemStack itemStack, ItemTransforms.TransformType transformType, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLightIn, int combinedOverlayIn) {
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);

        BakedModel model;
        if (!RingData.hasRingData(itemStack)) {
            model = renderer.getItemModelShaper().getModelManager().getModel(defaultModel);
        } else {
            ResourceLocation modelResource = getAffinityRingModelLocation(RingData.getRingData(itemStack).getSpell().getSchoolType().getId());
            model = renderer.getItemModelShaper().getModelManager().getModel(modelResource);
        }

        if (transformType == ItemTransforms.TransformType.GUI) {
            Lighting.setupForFlatItems();
            renderer.render(itemStack, transformType, false, poseStack, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
            Lighting.setupFor3DItems();
        } else {
            boolean leftHand = transformType == ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND || transformType == ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND;
            renderer.render(itemStack, transformType, leftHand, poseStack, bufferSource, combinedLightIn, combinedOverlayIn, model);
        }
        poseStack.popPose();
    }

    public static ResourceLocation getAffinityRingModelLocation(ResourceLocation schoolResource) {
        String namespace = schoolResource.getNamespace();
        String schoolName = schoolResource.getPath();
        return new ResourceLocation(namespace, "item/affinity_ring_" + schoolName);
    }
}
