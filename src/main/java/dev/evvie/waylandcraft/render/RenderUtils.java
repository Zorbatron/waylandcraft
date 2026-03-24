package dev.evvie.waylandcraft.render;

import java.io.IOException;
import java.util.function.Function;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;

import dev.evvie.waylandcraft.WaylandCraft;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

public class RenderUtils {
	
	private static ShaderInstance CUTOUT_NO_COLOR;
	private static ShaderInstance RENDERTYPE_WINDOW;
	
	public static void registerShaders(CoreShaderRegistrationCallback.RegistrationContext context) throws IOException {
		context.register(new ResourceLocation(WaylandCraft.MOD_ID, "cutout_no_color"), DefaultVertexFormat.POSITION_TEX, shader -> {
			CUTOUT_NO_COLOR = shader;
		});
		context.register(new ResourceLocation(WaylandCraft.MOD_ID, "rendertype_window"), DefaultVertexFormat.NEW_ENTITY, shader -> {
			RENDERTYPE_WINDOW = shader;
		});
	}
	
	public static ShaderInstance getCutoutNoColorShader() {
		return CUTOUT_NO_COLOR;
	}
	
	public static ShaderInstance getRendertypeWindowShader() {
		return RENDERTYPE_WINDOW;
	}
	
	public static RenderType window(int texture) {
		return DummyRenderType.WINDOW.apply(texture);
	}
	
	/* This whole subclass dummy is necessary to access the RenderType.CompositeState class */
	private static class DummyRenderType extends RenderType {
		
		public DummyRenderType(String string, VertexFormat vertexFormat, Mode mode, int i, boolean bl, boolean bl2, Runnable runnable, Runnable runnable2) {
			super(string, vertexFormat, mode, i, bl, bl2, runnable, runnable2);
			throw new IllegalStateException("DummyRenderType constructor called");
		}
		
		public static Function<Integer, RenderType> WINDOW = Util.memoize(DummyRenderType::window);
		public static final RenderStateShard.ShaderStateShard RENDERTYPE_WINDOW = new RenderStateShard.ShaderStateShard(RenderUtils::getRendertypeWindowShader);
		
		private static RenderType window(int texture) {
			RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
					.setShaderState(RENDERTYPE_WINDOW)
					.setTextureState(new TextureIdShard(texture))
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setOutputState(ITEM_ENTITY_TARGET)
					.setLightmapState(NO_LIGHTMAP)
					.setOverlayState(NO_OVERLAY)
					.setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
					.createCompositeState(true);
			return create("wlc_window", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, RenderType.TRANSIENT_BUFFER_SIZE, true, true, compositeState);
		}
		
		private static class TextureIdShard extends RenderStateShard.EmptyTextureStateShard {
			
			public TextureIdShard(int texture) {
				super(() -> {
					RenderSystem.setShaderTexture(0, texture);
				}, () -> {});
			}
			
		}
		
	}
	
	public static void blitGUIUnscaled(GuiGraphics graphics, int tex, float x1, float y1, float x2, float y2) {
		float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
		x1 /= guiScale;
		y1 /= guiScale;
		x2 /= guiScale;
		y2 /= guiScale;
		
		blitGUI(graphics, tex, x1, y1, x2, y2, 0, 0, 1, 1);
	}
	
	public static void blitGUI(GuiGraphics graphics, int tex, float x1, float y1, float x2, float y2) {
		blitGUI(graphics, tex, x1, y1, x2, y2, 0, 0, 1, 1);
	}
	
	public static void blitGUI(GuiGraphics graphics, int tex, float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2) {
		RenderSystem.setShaderTexture(0, tex);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		Matrix4f matrix4f = graphics.pose().last().pose();
		BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferBuilder.vertex(matrix4f, x1, y1, 0).uv(u1, v1).endVertex();
		bufferBuilder.vertex(matrix4f, x1, y2, 0).uv(u1, v2).endVertex();
		bufferBuilder.vertex(matrix4f, x2, y2, 0).uv(u2, v2).endVertex();
		bufferBuilder.vertex(matrix4f, x2, y1, 0).uv(u2, v1).endVertex();
		BufferUploader.drawWithShader(bufferBuilder.end());
	}
	
	public static void blitGUI(GuiGraphics graphics, ResourceLocation tex, float x1, float y1, float x2, float y2) {
		blitGUI(graphics, tex, x1, y1, x2, y2, 0, 0, 1, 1);
	}
	
	public static void blitGUI(GuiGraphics graphics, ResourceLocation tex, float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2) {
		RenderSystem.setShaderTexture(0, tex);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		Matrix4f matrix4f = graphics.pose().last().pose();
		BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferBuilder.vertex(matrix4f, x1, y1, 0).uv(u1, v1).endVertex();
		bufferBuilder.vertex(matrix4f, x1, y2, 0).uv(u1, v2).endVertex();
		bufferBuilder.vertex(matrix4f, x2, y2, 0).uv(u2, v2).endVertex();
		bufferBuilder.vertex(matrix4f, x2, y1, 0).uv(u2, v1).endVertex();
		BufferUploader.drawWithShader(bufferBuilder.end());
	}
	
	public static Pose cameraTransformPose(Camera camera) {
		PoseStack matrixStack = new PoseStack();
		matrixStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
		matrixStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
		matrixStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
		
		return matrixStack.last();
	}
	
}
