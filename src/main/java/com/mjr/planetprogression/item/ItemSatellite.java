package com.mjr.planetprogression.item;

import java.util.List;

import com.mjr.mjrlegendslib.item.BasicItem;
import com.mjr.mjrlegendslib.util.TranslateUtilities;
import com.mjr.planetprogression.PlanetProgression;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import micdoodle8.mods.galacticraft.core.util.EnumColor;

public class ItemSatellite extends BasicItem {

	private int type;

	public ItemSatellite(String name, int type) {
		super(name);
		this.type = type;
		this.setCreativeTab(PlanetProgression.tab);
		this.setMaxStackSize(1);
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack par1ItemStack, EntityPlayer player, List<String> par2List, boolean b) {
		par2List.add(EnumColor.AQUA + TranslateUtilities.translate("satellite.use.desc"));
		par2List.add(EnumColor.AQUA + TranslateUtilities.translate("satellite.use.desc.rename"));
	}
}
