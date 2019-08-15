package com.mjr.planetprogression.inventory;

import com.mjr.planetprogression.tileEntities.TileEntitySatelliteRocketLauncher;

import micdoodle8.mods.galacticraft.api.item.IItemElectric;
import micdoodle8.mods.galacticraft.core.energy.EnergyUtil;
import micdoodle8.mods.galacticraft.core.inventory.SlotSpecific;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerSatelliteRocketLauncher extends Container {
	private final TileEntitySatelliteRocketLauncher tileEntity;

	public ContainerSatelliteRocketLauncher(InventoryPlayer par1InventoryPlayer, TileEntitySatelliteRocketLauncher tileEntity, EntityPlayer player) {
		this.tileEntity = tileEntity;

		this.addSlotToContainer(new SlotSpecific(tileEntity, 0, 152, 105, IItemElectric.class));

		int var6;
		int var7;

		for (var6 = 0; var6 < 3; ++var6) {
			for (var7 = 0; var7 < 9; ++var7) {
				this.addSlotToContainer(new Slot(par1InventoryPlayer, var7 + var6 * 9 + 9, 8 + var7 * 18, 127 + var6 * 18));
			}
		}

		for (var6 = 0; var6 < 9; ++var6) {
			this.addSlotToContainer(new Slot(par1InventoryPlayer, var6, 8 + var6 * 18, 185));
		}

		tileEntity.openInventory(player);
	}

	@Override
	public void onContainerClosed(EntityPlayer entityplayer) {
		super.onContainerClosed(entityplayer);
		this.tileEntity.closeInventory(entityplayer);
	}

	@Override
	public boolean canInteractWith(EntityPlayer par1EntityPlayer) {
		return this.tileEntity.isUseableByPlayer(par1EntityPlayer);
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer entityPlayer, int par1) {
		ItemStack var2 = null;
		final Slot slot = this.inventorySlots.get(par1);
		final int b = this.inventorySlots.size();

		if (slot != null && slot.getHasStack()) {
			final ItemStack stack = slot.getStack();
			var2 = stack.copy();

			if (par1 == 0) {
				if (!this.mergeItemStack(stack, b - 36, b, true)) {
					return null;
				}
			} else {
				if (EnergyUtil.isElectricItem(stack.getItem())) {
					if (!this.mergeItemStack(stack, 0, 1, false)) {
						return null;
					}
				} else {
					if (par1 < b - 9) {
						if (!this.mergeItemStack(stack, b - 9, b, false)) {
							return null;
						}
					} else if (!this.mergeItemStack(stack, b - 36, b - 9, false)) {
						return null;
					}
				}
			}

			if (stack.stackSize == 0) {
				slot.putStack(null);
			} else {
				slot.onSlotChanged();
			}

			if (stack.stackSize == var2.stackSize) {
				return null;
			}

			slot.onPickupFromSlot(entityPlayer, stack);
		}

		return var2;
	}
}