package com.mjr.planetprogression.client.gui;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.mjr.mjrlegendslib.util.TranslateUtilities;
import com.mjr.planetprogression.Config;
import com.mjr.planetprogression.Constants;
import com.mjr.planetprogression.PlanetProgression;
import com.mjr.planetprogression.inventory.ContainerTelescope;
import com.mjr.planetprogression.network.PacketSimplePP;
import com.mjr.planetprogression.tileEntities.TileEntityTelescope;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;

import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.client.gui.container.GuiContainerGC;
import micdoodle8.mods.galacticraft.core.client.gui.element.GuiElementInfoRegion;
import micdoodle8.mods.galacticraft.core.energy.EnergyDisplayHelper;
import micdoodle8.mods.galacticraft.core.network.PacketSimple;
import micdoodle8.mods.galacticraft.core.network.PacketSimple.EnumSimplePacket;
import micdoodle8.mods.galacticraft.core.util.EnumColor;
import micdoodle8.mods.galacticraft.core.util.GCCoreUtil;

public class GuiTelescope extends GuiContainerGC {
	private static final ResourceLocation gui = new ResourceLocation(Constants.ASSET_PREFIX, "textures/gui/telescope.png");

	private TileEntityTelescope tileEntity;

	private GuiButton enableButton;
	private GuiButton leftButton;
	private GuiButton rightButton;

	private GuiElementInfoRegion electricInfoRegion = new GuiElementInfoRegion(0, 0, 52, 9, null, 0, 0, this);

	public GuiTelescope(InventoryPlayer playerInventory, TileEntityTelescope tileEntity) {
		super(new ContainerTelescope(playerInventory, tileEntity, FMLClientHandler.instance().getClient().thePlayer));
		this.xSize = 250;
		this.ySize = 230;
		this.tileEntity = tileEntity;
	}

	@Override
	public void drawScreen(int par1, int par2, float par3) {
		if (this.tileEntity.disableCooldown > 0) {
			this.enableButton.enabled = false;
		} else {
			this.enableButton.enabled = true;
		}

		this.enableButton.displayString = this.tileEntity.getDisabled(0) ? TranslateUtilities.translate("gui.button.enable.name") : TranslateUtilities.translate("gui.button.disable.name");

		super.drawScreen(par1, par2, par3);
	}

	@Override
	public void initGui() {
		super.initGui();
		this.buttonList.clear();
		final int var5 = (this.width - this.xSize) / 2;
		final int var6 = (this.height - this.ySize) / 2;
		this.enableButton = new GuiButton(0, var5 + 10, var6 + 110, 60, 20, TranslateUtilities.translate("gui.button.enable.name"));
		this.leftButton = new GuiButton(1, var5 + 5, var6 + 75, 15, 20, "<");
		this.rightButton = new GuiButton(2, var5 + 70 + 158, var6 + 75, 15, 20, ">");

		this.buttonList.add(this.enableButton);
		this.buttonList.add(this.leftButton);
		this.buttonList.add(this.rightButton);
		this.electricInfoRegion.tooltipStrings = new ArrayList<String>();
		this.electricInfoRegion.xPosition = (this.width - this.xSize) / 2 + 98;
		this.electricInfoRegion.yPosition = (this.height - this.ySize) / 2 + 113;
		this.electricInfoRegion.parentWidth = this.width;
		this.electricInfoRegion.parentHeight = this.height;
		this.infoRegions.add(this.electricInfoRegion);
		List<String> batterySlotDesc = new ArrayList<String>();
		batterySlotDesc.add(TranslateUtilities.translate("gui.battery_slot.desc.0"));
		this.infoRegions.add(new GuiElementInfoRegion((this.width - this.xSize) / 2 + 151, (this.height - this.ySize) / 2 + 104, 18, 18, batterySlotDesc, this.width, this.height, this));
	}

	@Override
	protected void actionPerformed(GuiButton par1GuiButton) {
		if (par1GuiButton.enabled) {
			switch (par1GuiButton.id) {
			case 0:
				GalacticraftCore.packetPipeline.sendToServer(new PacketSimple(EnumSimplePacket.S_UPDATE_DISABLEABLE_BUTTON, GCCoreUtil.getDimensionID(mc.theWorld), new Object[] { this.tileEntity.getPos(), 0 }));
				break;
			case 1:
				PlanetProgression.packetPipeline.sendToServer(new PacketSimplePP(com.mjr.planetprogression.network.PacketSimplePP.EnumSimplePacket.S_UPDATE_ROTATION, GCCoreUtil.getDimensionID(mc.theWorld), new Object[] { this.tileEntity.getPos(),
						0.0F }));
				break;
			case 2:
				PlanetProgression.packetPipeline.sendToServer(new PacketSimplePP(com.mjr.planetprogression.network.PacketSimplePP.EnumSimplePacket.S_UPDATE_ROTATION, GCCoreUtil.getDimensionID(mc.theWorld), new Object[] { this.tileEntity.getPos(),
						1.0F }));
				break;
			default:
				break;
			}
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int par1, int par2) {
		String displayString = this.tileEntity.getName();
		this.fontRendererObj.drawString(displayString, this.xSize / 2 - this.fontRendererObj.getStringWidth(displayString) / 2, 5, 4210752);
		this.fontRendererObj.drawString(TranslateUtilities.translate("container.inventory"), 8, 135, 4210752);
		this.fontRendererObj.drawString("Progress: " + (int) (((this.tileEntity.processTicks / Config.telescopeTimeModifier) / 2) * 4) / 2 + " %", 5, 20, 4210752);
		this.fontRendererObj.drawString("Player: " + ((this.tileEntity.owner != "" && this.tileEntity.ownerOnline) ? this.tileEntity.ownerUsername : TranslateUtilities.translate("gui.telescope.no_player.name")), 5, 55, 4210752);
		String displayText = "Unknown";
		if (!this.tileEntity.hasInputs()) {
			displayText = EnumColor.RED + TranslateUtilities.translate("gui.telescope.status.missing.paper.name");
		} else if (!this.tileEntity.hasEnoughEnergyToRun) {
			displayText = EnumColor.RED + TranslateUtilities.translate("gui.status.missing.power.name");
		} else if (this.tileEntity.alreadyResearchedInput)
			displayText = TranslateUtilities.translate("gui.telescope.status.already_researched.name");
		else
			displayText = TranslateUtilities.translate("gui.telescope.status.normal.name");
		this.fontRendererObj.drawString("Status: " + displayText, 5, 30, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
		GL11.glPushMatrix();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.renderEngine.bindTexture(GuiTelescope.gui);
		final int var5 = (this.width - this.xSize) / 2;
		final int var6 = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(var5, var6, 0, 0, this.xSize, this.ySize);

		List<String> electricityDesc = new ArrayList<String>();
		electricityDesc.add(TranslateUtilities.translate("gui.energy_storage.desc.0"));
		EnergyDisplayHelper.getEnergyDisplayTooltip(this.tileEntity.getEnergyStoredGC(), this.tileEntity.getMaxEnergyStoredGC(), electricityDesc);
		this.electricInfoRegion.tooltipStrings = electricityDesc;

		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		if (this.tileEntity.getEnergyStoredGC() > 0) {
			int scale = this.tileEntity.getScaledElecticalLevel(54);
			this.drawTexturedModalRect(var5 + 92, var6 + 111, 0, 249, Math.min(scale, 54), 7);
		}

		GL11.glPopMatrix();
	}
}