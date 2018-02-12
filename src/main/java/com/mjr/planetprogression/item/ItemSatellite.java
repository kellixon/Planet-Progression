package com.mjr.planetprogression.item;

import java.util.List;

import javax.annotation.Nullable;

import micdoodle8.mods.galacticraft.core.util.EnumColor;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.mjr.mjrlegendslib.item.BasicItem;
import com.mjr.mjrlegendslib.util.TranslateUtilities;
import com.mjr.planetprogression.PlanetProgression;

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
	public void addInformation(ItemStack itemStack, @Nullable World worldIn, List<String> par2List, ITooltipFlag flagIn) {
		par2List.add(EnumColor.AQUA + TranslateUtilities.translate("satellite.use.desc"));
	}
}
