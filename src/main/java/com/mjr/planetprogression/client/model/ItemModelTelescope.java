package com.mjr.planetprogression.client.model;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import org.lwjgl.opengl.GL11;

import com.mjr.mjrlegendslib.client.model.ModelTransformWrapper;
import com.mjr.planetprogression.Constants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraftforge.client.model.TRSRTransformation;

public class ItemModelTelescope extends ModelTransformWrapper {
	public ItemModelTelescope(IBakedModel modelToWrap) {
		super(modelToWrap);
	}

	@Override
	protected Matrix4f getTransformForPerspective(TransformType cameraTransformType) {
		if (cameraTransformType == TransformType.GUI) {
			Matrix4f ret = new Matrix4f();
			ret.setIdentity();
			Matrix4f mul = new Matrix4f();
			mul.setIdentity();
			mul.setTranslation(new Vector3f(-0.3F, -0.65F, 0.2F));
			ret.mul(mul);
			mul.setIdentity();
			mul.rotX((float) Math.PI / 12.0F);
			ret.mul(mul);
			mul.setIdentity();
			mul.rotY((float) Math.PI / 30.0F);
			ret.mul(mul);
			mul.setIdentity();
			mul.setScale(0.030F);
			ret.mul(mul);
			return ret;
		}

		if (cameraTransformType == TransformType.FIRST_PERSON) {
			if (Minecraft.isAmbientOcclusionEnabled()) {
				GlStateManager.shadeModel(GL11.GL_SMOOTH);
			} else {
				GlStateManager.shadeModel(GL11.GL_FLAT);
			}
			Vector3f trans = new Vector3f(0.0F, 10F, 6.0F);
			Matrix4f ret = new Matrix4f();
			ret.setIdentity();
			Matrix4f mul = new Matrix4f();
			mul.setIdentity();
			Quat4f rot = TRSRTransformation.quatFromYXZDegrees(new Vector3f(140, 100, 0));
			mul.setRotation(rot);
			ret.mul(mul);
			mul.setIdentity();
			mul.setScale(0.03F);
			ret.mul(mul);
			mul.setIdentity();
			mul.rotX(Constants.halfPI + 1.0F);
			ret.mul(mul);
			mul.setIdentity();
			mul.rotZ((float) (-0.65F + Math.PI));
			ret.mul(mul);
			mul.setIdentity();
			mul.setTranslation(trans);
			ret.mul(mul);
			mul.setIdentity();
			mul.rotX((float) (Math.PI - 1.0F));
			ret.mul(mul);
			return ret;
		}

		if (cameraTransformType == TransformType.THIRD_PERSON) {
			if (Minecraft.isAmbientOcclusionEnabled()) {
				GlStateManager.shadeModel(GL11.GL_SMOOTH);
			} else {
				GlStateManager.shadeModel(GL11.GL_FLAT);
			}
			Vector3f trans = new Vector3f(-9.0F, -1.5F, 2.5F);
			Matrix4f ret = new Matrix4f();
			ret.setIdentity();
			Matrix4f mul = new Matrix4f();
			mul.setIdentity();
			Quat4f rot = TRSRTransformation.quatFromYXZDegrees(new Vector3f(180, 95, 90));
			mul.setRotation(rot);
			ret.mul(mul);
			mul.setIdentity();
			mul.setScale(0.05F);
			ret.mul(mul);
			mul.setIdentity();
			mul.rotX((float) (Math.PI / 3.0F));
			ret.mul(mul);
			mul.setIdentity();
			mul.rotZ((float) (-Math.PI / 2.0F));
			ret.mul(mul);
			mul.setIdentity();
			mul.rotX(0.3F);
			ret.mul(mul);
			mul.setIdentity();
			mul.setTranslation(trans);
			ret.mul(mul);
			return ret;
		}

		if (cameraTransformType == TransformType.GROUND) {
			Matrix4f ret = new Matrix4f();
			ret.setIdentity();
			Matrix4f mul = new Matrix4f();
			mul.setIdentity();
			mul.setScale(0.05F);
			ret.mul(mul);
			mul.setIdentity();
			mul.setTranslation(new Vector3f(0.05F, 0.0F, 0.05F));
			ret.mul(mul);
			return ret;
		}
		if (cameraTransformType == TransformType.FIXED) {
			Matrix4f ret = new Matrix4f();
			ret.setIdentity();
			Matrix4f mul = new Matrix4f();
			mul.setIdentity();
			mul.setScale(1.25F);
			ret.mul(mul);
			mul.setIdentity();
			mul.rotY(1.575F);
			ret.mul(mul);
			mul.setIdentity();
			mul.setTranslation(new Vector3f(0.1F, -0.1F, 0.05F));
			ret.mul(mul);
			return ret;
		}
		return null;
	}
}
