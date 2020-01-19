package com.mjr.planetprogression.entities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import io.netty.buffer.ByteBuf;

import com.mjr.planetprogression.client.sounds.SoundUpdaterSatelliteRocket;

import net.minecraft.block.Block;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import micdoodle8.mods.galacticraft.api.entity.IDockable;
import micdoodle8.mods.galacticraft.api.entity.IEntityNoisy;
import micdoodle8.mods.galacticraft.api.entity.ILandable;
import micdoodle8.mods.galacticraft.api.entity.IRocketType;
import micdoodle8.mods.galacticraft.api.prefab.entity.EntitySpaceshipBase;
import micdoodle8.mods.galacticraft.api.tile.IFuelDock;
import micdoodle8.mods.galacticraft.api.tile.ILandingPadAttachable;
import micdoodle8.mods.galacticraft.api.vector.BlockVec3;
import micdoodle8.mods.galacticraft.api.world.IGalacticraftWorldProvider;
import micdoodle8.mods.galacticraft.api.world.IOrbitDimension;
import micdoodle8.mods.galacticraft.core.Constants;
import micdoodle8.mods.galacticraft.core.GCBlocks;
import micdoodle8.mods.galacticraft.core.GCFluids;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.blocks.BlockLandingPadFull;
import micdoodle8.mods.galacticraft.core.client.sounds.SoundUpdaterRocket;
import micdoodle8.mods.galacticraft.core.entities.player.GCPlayerStats;
import micdoodle8.mods.galacticraft.core.event.EventLandingPadRemoval;
import micdoodle8.mods.galacticraft.core.inventory.IInventoryDefaults;
import micdoodle8.mods.galacticraft.core.network.PacketDynamic;
import micdoodle8.mods.galacticraft.core.tile.TileEntityLandingPad;
import micdoodle8.mods.galacticraft.core.util.*;

/**
 * Custom/Edited Version of EntityAutoRocket class from Galacticraft
 */
public abstract class EntitySatelliteAutoRocket extends EntitySpaceshipBase implements ILandable, IInventoryDefaults, IEntityNoisy, IRocketType, IInventory, IDockable {
	public static enum EnumAutoLaunch {
		INSTANT(0, "instant"), TIME_10_SECONDS(1, "ten_sec"), TIME_30_SECONDS(3, "thirty_sec"), TIME_1_MINUTE(4, "one_min"), REDSTONE_SIGNAL(5, "redstone_sig");

		private final int index;
		private String title;

		private EnumAutoLaunch(int index, String title) {
			this.index = index;
			this.title = title;
		}

		public int getIndex() {
			return this.index;
		}

		public String getTitle() {
			return GCCoreUtil.translate("gui.message." + this.title + ".name");
		}
	}

	private static Class<?> controllerClass = null;
	static {
		try {
			controllerClass = Class.forName("com.mjr.planetprogression.tileEntities.TileEntitySatelliteRocketLauncher");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	protected NonNullList<ItemStack> stacks;
	private IFuelDock landingPad;
	public EnumAutoLaunch autoLaunchSetting;

	public int autoLaunchCountdown;
	private BlockVec3 activeLaunchController;
	public String placedPlayerUUID;
	public String statusMessage;
	public String statusColour;
	public int statusMessageCooldown;
	public int lastStatusMessageCooldown;
	public boolean statusValid;
	protected double lastMotionY;
	protected double lastLastMotionY;
	private boolean waitForPlayer;

	protected ITickable rocketSoundUpdater;

	private boolean rocketSoundToStop = false;

	public EntitySatelliteAutoRocket(World world) {
		super(world);

		if (world != null && world.isRemote) {
			GalacticraftCore.packetPipeline.sendToServer(new PacketDynamic(this));
		}
	}

	public EntitySatelliteAutoRocket(World world, double posX, double posY, double posZ) {
		this(world);
		this.setSize(0.98F, 2F);
		this.setPosition(posX, posY, posZ);
		this.motionX = 0.0D;
		this.motionY = 0.0D;
		this.motionZ = 0.0D;
		this.prevPosX = posX;
		this.prevPosY = posY;
		this.prevPosZ = posZ;
	}

	@Override
	public EnumCargoLoadingState addCargo(ItemStack stack, boolean doAdd) {
		// if (this.getSizeInventory() <= 3) {
		// if (this.autoLaunchSetting == EnumAutoLaunch.CARGO_IS_FULL) {
		// this.autoLaunch();
		// }
		//
		// return EnumCargoLoadingState.NOINVENTORY;
		// }

		int count = 0;

		for (count = 0; count < this.stacks.size() - 2; count++) {
			ItemStack stackAt = this.stacks.get(count);

			if (!stackAt.isEmpty() && stackAt.getItem() == stack.getItem() && stackAt.getItemDamage() == stack.getItemDamage() && stackAt.getCount() < stackAt.getMaxStackSize()) {
				if (stackAt.getCount() + stack.getCount() <= stackAt.getMaxStackSize()) {
					if (doAdd) {
						stackAt.grow(stack.getCount());
						this.markDirty();
					}

					return EnumCargoLoadingState.SUCCESS;
				} else {
					// Part of the stack can fill this slot but there will be some left over
					int origSize = stackAt.getCount();
					int surplus = origSize + stack.getCount() - stackAt.getMaxStackSize();

					if (doAdd) {
						stackAt.setCount(stackAt.getMaxStackSize());
						this.markDirty();
					}

					stack.setCount(surplus);
					if (this.addCargo(stack, doAdd) == EnumCargoLoadingState.SUCCESS) {
						return EnumCargoLoadingState.SUCCESS;
					}

					stackAt.setCount(origSize);
					// if (this.autoLaunchSetting == EnumAutoLaunch.CARGO_IS_FULL) {
					// this.autoLaunch();
					// }
					return EnumCargoLoadingState.FULL;
				}
			}
		}

		for (count = 0; count < this.stacks.size() - 2; count++) {
			ItemStack stackAt = this.stacks.get(count);

			if (stackAt.isEmpty()) {
				if (doAdd) {
					this.stacks.set(count, stack);
					this.markDirty();
				}

				return EnumCargoLoadingState.SUCCESS;
			}
		}

		// if (this.autoLaunchSetting == EnumAutoLaunch.CARGO_IS_FULL) {
		// this.autoLaunch();
		// }

		return EnumCargoLoadingState.FULL;
	}

	@Override
	public int addFuel(FluidStack liquid, boolean doFill) {
		return FluidUtil.fillWithGCFuel(this.fuelTank, liquid, doFill);
	}

	// Server side only - this is a Launch Controlled ignition attempt
	private void autoLaunch() {
		if (this.autoLaunchSetting != null) {
			if (this.activeLaunchController != null) {
				TileEntity tile = this.activeLaunchController.getTileEntity(this.world);

				if (controllerClass.isInstance(tile)) {
					Boolean autoLaunchEnabled = null;
					try {
						;
						autoLaunchEnabled = controllerClass.getField("launchEnabled").getBoolean(tile);
					} catch (Exception e) {
					}

					if (autoLaunchEnabled != null && autoLaunchEnabled) {
						if (this.fuelTank.getFluidAmount() > this.fuelTank.getCapacity() * 2 / 5) {
							this.ignite();
						} else
							this.failMessageInsufficientFuel();
					} else {
						this.failMessageLaunchController();
					}
				}
			}
			this.autoLaunchSetting = null;

			return;
		} else {
			this.ignite();
		}
	}

	public void cancelLaunch() {
		this.setLaunchPhase(EnumLaunchPhase.UNIGNITED);
		this.timeUntilLaunch = 0;
		if (!this.world.isRemote && !this.getPassengers().isEmpty() && this.getPassengers().get(0) instanceof EntityPlayerMP) {
			this.getPassengers().get(0).sendMessage(new TextComponentString(GCCoreUtil.translate("gui.rocket.warning.nogyroscope")));
		}
	}

	// Used for Cargo Rockets, client is asking the server what is the status
	public boolean checkLaunchValidity() {
		this.statusMessageCooldown = 40;

		if (this.hasValidFuel()) {
			if (this.launchPhase == EnumLaunchPhase.UNIGNITED.ordinal() && !this.world.isRemote) {
				this.statusMessage = I18n.translateToLocal("gui.message.success.name");
				this.statusColour = "\u00a7a";
				return true;
			}
		} else {
			this.statusMessage = I18n.translateToLocal("gui.message.not_enough.name") + "#" + I18n.translateToLocal("gui.message.fuel.name");
			this.statusColour = "\u00a7c";
			return false;
		}

		return false;
	}

	@Override
	public void decodePacketdata(ByteBuf buffer) {
		if (!this.world.isRemote) {
			double clientPosY = buffer.readDouble();
			if (clientPosY != Double.NaN && this.hasValidFuel()) {
				if (this.launchPhase == EnumLaunchPhase.LAUNCHED.ordinal()) {
					if (clientPosY > this.posY) {
						this.motionY += (clientPosY - this.posY) / 40D;
					}
				} else if (this.launchPhase == EnumLaunchPhase.LANDING.ordinal()) {
					if (clientPosY < this.posY) {
						this.motionY += (clientPosY - this.posY) / 40D;
					}
				}
			}
			return;
		}
		int launchPhaseLast = this.launchPhase;
		super.decodePacketdata(buffer);
		this.fuelTank.setFluid(new FluidStack(GCFluids.fluidFuel, buffer.readInt()));
		boolean landingNew = buffer.readBoolean();
		if (landingNew && launchPhaseLast != EnumLaunchPhase.LANDING.ordinal()) {
			this.rocketSoundUpdater = null; // Flag TickHandlerClient to restart it
		}

		double motX = buffer.readDouble() / 8000.0D;
		double motY = buffer.readDouble() / 8000.0D;
		double motZ = buffer.readDouble() / 8000.0D;
		double lastMotY = buffer.readDouble() / 8000.0D;
		double lastLastMotY = buffer.readDouble() / 8000.0D;

		if (!this.hasValidFuel()) {
			this.motionX = motX;
			this.motionY = motY;
			this.motionZ = motZ;
			this.lastMotionY = lastMotY;
			this.lastLastMotionY = lastLastMotY;
		}

		if (this.stacks == null) {
			this.stacks = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
		}

		this.setWaitForPlayer(buffer.readBoolean());

		this.statusMessage = ByteBufUtils.readUTF8String(buffer);
		this.statusMessage = this.statusMessage.equals("") ? null : this.statusMessage;
		this.statusMessageCooldown = buffer.readInt();
		this.lastStatusMessageCooldown = buffer.readInt();
		this.statusValid = buffer.readBoolean();

		// Update client with correct rider if needed
		if (this.world.isRemote) {
			int shouldBeMountedId = buffer.readInt();
			if (this.getPassengers().isEmpty()) {
				if (shouldBeMountedId > -1) {
					Entity e = FMLClientHandler.instance().getWorldClient().getEntityByID(shouldBeMountedId);
					if (e != null) {
						if (e.dimension != this.dimension) {
							if (e instanceof EntityPlayer) {
								e = WorldUtil.forceRespawnClient(this.dimension, e.world.getDifficulty().getDifficultyId(), e.world.getWorldInfo().getTerrainType().getName(), ((EntityPlayerMP) e).interactionManager.getGameType().getID());
								e.startRiding(this);
							}
						} else
							e.startRiding(this);
					}
				}
			} else if (this.getPassengers().get(0).getEntityId() != shouldBeMountedId) {
				if (shouldBeMountedId == -1) {
					this.removePassengers();
				} else {
					Entity e = FMLClientHandler.instance().getWorldClient().getEntityByID(shouldBeMountedId);
					if (e != null) {
						if (e.dimension != this.dimension) {
							if (e instanceof EntityPlayer) {
								e = WorldUtil.forceRespawnClient(this.dimension, e.world.getDifficulty().getDifficultyId(), e.world.getWorldInfo().getTerrainType().getName(), ((EntityPlayerMP) e).interactionManager.getGameType().getID());
								e.startRiding(this);
							}
						} else
							e.startRiding(this);
					}
				}
			}
		}
		this.statusColour = ByteBufUtils.readUTF8String(buffer);
		if (this.statusColour.equals(""))
			this.statusColour = null;
	}

	@Override
	public ItemStack decrStackSize(int index, int count) {
		ItemStack itemstack = ItemStackHelper.getAndSplit(this.stacks, index, count);

		if (!itemstack.isEmpty()) {
			this.markDirty();
		}

		return itemstack;
	}

	public void failMessageInsufficientFuel() {
		if (!this.world.isRemote && !this.getPassengers().isEmpty() && this.getPassengers().get(0) instanceof EntityPlayerMP) {
			((EntityPlayerMP) this.getPassengers().get(0)).sendMessage(new TextComponentString(GCCoreUtil.translate("gui.rocket.warning.fuelinsufficient")));
		}
	}

	public void failMessageLaunchController() {
		if (!this.world.isRemote && !this.getPassengers().isEmpty() && this.getPassengers().get(0) instanceof EntityPlayerMP) {
			((EntityPlayerMP) this.getPassengers().get(0)).sendMessage(new TextComponentString(GCCoreUtil.translate("gui.rocket.warning.launchcontroller")));
		}
	}

	@Override
	protected void failRocket() {
		this.stopRocketSound();

		if (this.shouldCancelExplosion()) {
			// TODO: why looking around when already know the target?
			// TODO: it would be good to land on an alternative neighbouring pad if there is already a rocket on the target pad
			for (int i = -3; i <= 3; i++) {
				BlockPos pos = new BlockPos((int) Math.floor(this.posX), (int) Math.floor(this.posY + i), (int) Math.floor(this.posZ));
				if (this.launchPhase == EnumLaunchPhase.LANDING.ordinal() && this.world.getTileEntity(pos) instanceof IFuelDock && this.posY < 5) {
					for (int x = MathHelper.floor(this.posX) - 1; x <= MathHelper.floor(this.posX) + 1; x++) {
						for (int y = MathHelper.floor(this.posY - 3.0D); y <= MathHelper.floor(this.posY) + 1; y++) {
							for (int z = MathHelper.floor(this.posZ) - 1; z <= MathHelper.floor(this.posZ) + 1; z++) {
								BlockPos pos1 = new BlockPos(x, y, z);
								TileEntity tile = this.world.getTileEntity(pos1);

								if (tile instanceof IFuelDock) {
									this.landEntity(pos1);
									return;
								}
							}
						}
					}
				}
			}
		}

		if (this.launchPhase >= EnumLaunchPhase.LAUNCHED.ordinal()) {
			super.failRocket();
		}
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public List<ItemStack> getItemsDropped(List<ItemStack> droppedItemList) {
		if (this.stacks != null) {
			for (final ItemStack item : this.stacks) {
				if (item != null && !item.isEmpty()) {
					droppedItemList.add(item);
				}
			}
		}

		return droppedItemList;
	}

	@Override
	public IFuelDock getLandingPad() {
		return this.landingPad;
	}

	@Override
	public int getMaxFuel() {
		return this.fuelTank.getCapacity();
	}

	@Override
	public void getNetworkedData(ArrayList<Object> list) {
		if (this.world.isRemote) {
			if (this.getPassengers().contains(FMLClientHandler.instance().getClientPlayerEntity()) && this.hasValidFuel()) {
				list.add(this.posY);
			} else {
				list.add(Double.NaN);
			}
		}
		super.getNetworkedData(list);

		list.add(this.fuelTank.getFluidAmount());
		list.add(this.launchPhase == EnumLaunchPhase.LANDING.ordinal());

		list.add(this.motionX * 8000.0D);
		list.add(this.motionY * 8000.0D);
		list.add(this.motionZ * 8000.0D);
		list.add(this.lastMotionY * 8000.0D);
		list.add(this.lastLastMotionY * 8000.0D);

		list.add(this.getWaitForPlayer());

		list.add(this.statusMessage != null ? this.statusMessage : "");
		list.add(this.statusMessageCooldown);
		list.add(this.lastStatusMessageCooldown);
		list.add(this.statusValid);

		if (!this.world.isRemote) {
			list.add(this.getPassengers().isEmpty() ? -1 : this.getPassengers().get(0).getEntityId());
		}
		list.add(this.statusColour != null ? this.statusColour : "");
	}

	@Override
	public int getScaledFuelLevel(int scale) {
		if (this.getFuelTankCapacity() <= 0) {
			return 0;
		}

		return this.fuelTank.getFluidAmount() * scale / this.getFuelTankCapacity() / ConfigManagerCore.rocketFuelFactor;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ITickable getSoundUpdater() {
		return this.rocketSoundUpdater;
	}

	@Override
	public ItemStack getStackInSlot(int index) {
		if (this.stacks == null) {
			return ItemStack.EMPTY;
		}

		return this.stacks.get(index);
	}

	public boolean getWaitForPlayer() {
		return this.waitForPlayer;
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public boolean hasValidFuel() {
		return this.fuelTank.getFluidAmount() > 0;
	}

	@Override
	public void ignite() {
		this.igniteWithResult();
	}

	public boolean igniteWithResult() {
		super.ignite();
		this.activeLaunchController = null;
		return true;
	}

	@Override
	public boolean inFlight() {
		return this.getLaunched();
	}

	@Override
	public boolean isDockValid(IFuelDock dock) {
		return (dock instanceof TileEntityLandingPad);
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.stacks) {
			if (!itemstack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isInRangeToRender3d(double x, double y, double z) {
		double d0 = this.posX - x;
		double d1 = this.posY - y;
		double d2 = this.posZ - z;
		double d3 = d0 * d0 + d1 * d1 + d2 * d2;
		return d3 < Constants.RENDERDISTANCE_LONG;
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack) {
		return false;
	}

	public boolean isPlayerRocket() {
		return false;
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer entityplayer) {
		return !this.isDead && entityplayer.getDistanceSqToEntity(this) <= 64.0D;
	}

	@Override
	public void landEntity(BlockPos pos) {
		TileEntity tile = this.world.getTileEntity(pos);

		if (tile instanceof IFuelDock) {
			IFuelDock dock = (IFuelDock) tile;

			if (this.isDockValid(dock)) {
				if (!this.world.isRemote) {
					// Drop any existing rocket on the landing pad
					if (dock.getDockedEntity() instanceof EntitySpaceshipBase && dock.getDockedEntity() != this) {
						((EntitySpaceshipBase) dock.getDockedEntity()).dropShipAsItem();
						((EntitySpaceshipBase) dock.getDockedEntity()).setDead();
					}

					this.setPad(dock);
				}

				this.onRocketLand(pos);
			}
		}
	}

	@Override
	public void markDirty() {
	}

	@Override
	public void onLaunch() {
		if (!(this.world.provider.getDimension() == GalacticraftCore.planetOverworld.getDimensionID() || this.world.provider instanceof IGalacticraftWorldProvider)) {
			if (ConfigManagerCore.disableRocketLaunchAllNonGC) {
				this.cancelLaunch();
				return;
			}

			// No rocket flight in the Nether, the End etc
			for (int i = ConfigManagerCore.disableRocketLaunchDimensions.length - 1; i >= 0; i--) {
				if (ConfigManagerCore.disableRocketLaunchDimensions[i] == this.world.provider.getDimension()) {
					this.cancelLaunch();
					return;
				}
			}

		}

		super.onLaunch();

		if (!this.world.isRemote) {
			GCPlayerStats stats = null;

			if (!this.getPassengers().isEmpty()) {
				for (Entity player : this.getPassengers()) {
					if (player instanceof EntityPlayerMP) {
						stats = GCPlayerStats.get(player);
						stats.setLaunchpadStack(null);

						if (!(this.world.provider instanceof IOrbitDimension)) {
							stats.setCoordsTeleportedFromX(player.posX);
							stats.setCoordsTeleportedFromZ(player.posZ);
						}
					}
				}

				Entity playerMain = this.getPassengers().get(0);
				if (playerMain instanceof EntityPlayerMP)
					stats = GCPlayerStats.get(playerMain);
			}

			int amountRemoved = 0;

			PADSEARCH: for (int x = MathHelper.floor(this.posX) - 1; x <= MathHelper.floor(this.posX) + 1; x++) {
				for (int y = MathHelper.floor(this.posY) - 3; y <= MathHelper.floor(this.posY) + 1; y++) {
					for (int z = MathHelper.floor(this.posZ) - 1; z <= MathHelper.floor(this.posZ) + 1; z++) {
						BlockPos pos = new BlockPos(x, y, z);
						final Block block = this.world.getBlockState(pos).getBlock();

						if (block != null && block instanceof BlockLandingPadFull) {
							if (amountRemoved < 9) {
								EventLandingPadRemoval event = new EventLandingPadRemoval(this.world, pos);
								MinecraftForge.EVENT_BUS.post(event);

								if (event.allow) {
									this.world.setBlockToAir(pos);
									amountRemoved = 9;
								}
								break PADSEARCH;
							}
						}
					}
				}
			}

			// Set the player's launchpad item for return on landing - or null if launchpads not removed
			if (stats != null && amountRemoved == 9) {
				stats.setLaunchpadStack(new ItemStack(GCBlocks.landingPad, 9, 0));
			}

			this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.2F, ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
		}
	}

	@Override
	public void onPadDestroyed() {
		if (!this.isDead && this.launchPhase < EnumLaunchPhase.LAUNCHED.ordinal()) {
			this.dropShipAsItem();
			this.setDead();
		}
	}

	protected void onRocketLand(BlockPos pos) {
		this.setPositionAndRotation(pos.getX() + 0.5, pos.getY() + 0.4D + this.getOnPadYOffset(), pos.getZ() + 0.5, this.rotationYaw, 0.0F);
		this.stopRocketSound();
		this.rocketSoundUpdater = null; // Allow sound to be restarted again if it relaunches
	}

	@Override
	public void onUpdate() {
		// Workaround for a weird bug (?) in vanilla 1.8.9 where - if client view distance is shorter
		// than the server's chunk loading distance - chunks will unload on the client, but the
		// entity will still be in the WorldClient.loadedEntityList. This results in an entity which
		// is in the world, not dead and still updating on both server and client, but not in any chunk's
		// chunk.entityLists. Also, it won't be reloaded into any chunk's entityLists when the chunk comes
		// back into viewing range - not sure why, maybe because it's already in the World.loadedEntityList?
		// Because it's now not in any chunk's entityLists, it cannot be iterated for rendering by RenderGlobal,
		// so it's gone invisible!

		// Tracing shows that Chunk.onChunkUnload() is called on the chunk clientside when the chunk goes
		// out of the view distance. However, even after Chunk.onChunkUnload() - which should remove
		// this entity from the WorldClient.loadedEntityList - this entity stays in the world loadedEntityList.
		// That's why an onUpdate() tick is active for it, still!
		// Weird, huh?
		if (this.world.isRemote && this.addedToChunk && !CompatibilityManager.isCubicChunksLoaded) {
			Chunk chunk = this.world.getChunkFromChunkCoords(this.chunkCoordX, this.chunkCoordZ);
			int cx = MathHelper.floor(this.posX) >> 4;
			int cz = MathHelper.floor(this.posZ) >> 4;
			if (chunk.isLoaded() && this.chunkCoordX == cx && this.chunkCoordZ == cz) {
				boolean thisfound = false;
				ClassInheritanceMultiMap<Entity> mapEntities = chunk.getEntityLists()[this.chunkCoordY];
				for (Entity ent : mapEntities) {
					if (ent == this) {
						thisfound = true;
						break;
					}
				}
				if (!thisfound) {
					chunk.addEntity(this);
				}
			}
		}

		super.onUpdate();
		if (!this.world.isRemote) {
			if (this.statusMessageCooldown > 0) {
				this.statusMessageCooldown--;
			}

			if (this.statusMessageCooldown == 0 && this.lastStatusMessageCooldown > 0 && this.statusValid) {
				this.autoLaunch();
			}

			if (this.autoLaunchCountdown > 0 && this.getPassengers().isEmpty() && this.fuelTank.getFluidAmount() == this.fuelTank.getCapacity()) {
				if (--this.autoLaunchCountdown == 0) {
					this.autoLaunch();
				}
			}

			// if (this.autoLaunchSetting == EnumAutoLaunch.ROCKET_IS_FUELED && this.fuelTank.getFluidAmount() == this.fuelTank.getCapacity() && (!this.getPassengers().isEmpty())) {
			// this.autoLaunch();
			// }
			//
			// if (this.autoLaunchSetting == EnumAutoLaunch.INSTANT) {
			// if (this.autoLaunchCountdown == 0 && (this.getPassengers().isEmpty())) {
			// this.autoLaunch();
			// }
			// else if (this.fuelTank.getFluidAmount() == this.fuelTank.getCapacity())
			// placedPlayer.sendMessage(new TextComponentString( "Launcing in " + (this.autoLaunchCountdown / 10) + " seconds!"));
			// }
			//
			// if (this.autoLaunchSetting == EnumAutoLaunch.REDSTONE_SIGNAL) {
			// if (this.ticks % 11 == 0 && this.activeLaunchController != null) {
			// if (RedstoneUtil.isBlockReceivingRedstone(this.world, this.activeLaunchController.toBlockPos())) {
			// this.autoLaunch();
			// }
			// }
			// }

			if (this.launchPhase >= EnumLaunchPhase.LAUNCHED.ordinal()) {
				this.setPad(null);
			} else {
				if (this.launchPhase == EnumLaunchPhase.UNIGNITED.ordinal() && this.landingPad != null && this.ticks % 17 == 0) {
					this.updateControllerSettings(this.landingPad);
				}
			}

			this.lastStatusMessageCooldown = this.statusMessageCooldown;
		}

		if (this.launchPhase >= EnumLaunchPhase.IGNITED.ordinal()) {
			if (this.rocketSoundUpdater != null) {
				this.rocketSoundUpdater.update();
				this.rocketSoundToStop = true;
			}
		} else {
			if (this.rocketSoundToStop) {
				this.stopRocketSound();
				this.rocketSoundUpdater = null;
			}
		}
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);

		if (nbt.hasKey("fuelTank")) {
			this.fuelTank.readFromNBT(nbt.getCompoundTag("fuelTank"));
		}

		if (this.getSizeInventory() > 0) {
			this.stacks = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
			ItemStackHelper.loadAllItems(nbt, this.stacks);
		}

		this.setWaitForPlayer(nbt.getBoolean("WaitingForPlayer"));
		int autoLaunchValue = nbt.getInteger("AutoLaunchSetting");
		this.autoLaunchSetting = autoLaunchValue == -1 ? null : EnumAutoLaunch.values()[autoLaunchValue];
		this.autoLaunchCountdown = nbt.getInteger("TimeUntilAutoLaunch");
		this.activeLaunchController = BlockVec3.readFromNBT(nbt, "ALCat");
	}

	@Override
	public RemovalResult removeCargo(boolean doRemove) {
		// for (int i = 0; i < this.stacks.size() - 2; i++) {
		// ItemStack stackAt = this.stacks.get(i);
		//
		// if (!stackAt.isEmpty()) {
		// ItemStack resultStack = stackAt.copy();
		// resultStack.setCount(1);
		//
		// if (doRemove) {
		// stackAt.shrink(1);
		// }
		//
		// if (doRemove) {
		// this.markDirty();
		// }
		// return new RemovalResult(EnumCargoLoadingState.SUCCESS, resultStack);
		// }
		// }
		//
		// if (this.autoLaunchSetting == EnumAutoLaunch.CARGO_IS_UNLOADED) {
		// this.autoLaunch();
		// }

		return new RemovalResult(EnumCargoLoadingState.EMPTY, ItemStack.EMPTY);
	}

	@Override
	public FluidStack removeFuel(int amount) {
		return this.fuelTank.drain(amount * ConfigManagerCore.rocketFuelFactor, true);
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		return ItemStackHelper.getAndRemove(this.stacks, index);
	}

	@Override
	public void setDead() {
		super.setDead();

		if (this.rocketSoundUpdater != null) {
			this.rocketSoundUpdater.update();
		}
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		this.stacks.set(index, stack);

		if (stack.getCount() > this.getInventoryStackLimit()) {
			stack.setCount(this.getInventoryStackLimit());
		}

		this.markDirty();
	}

	@Override
	public void setPad(IFuelDock pad) {
		// Called either when a rocket lands or when one is placed
		// Can also be called with null param when rocket leaves a pad
		this.landingPad = pad;
		if (pad != null) {
			pad.dockEntity(this);
			// NOTE: setPad() is called also when a world or chunk is loaded - if the rocket is Ignited (from NBT save data) do not change those settings
			if (!(this.launchPhase == EnumLaunchPhase.IGNITED.ordinal())) {
				this.setLaunchPhase(EnumLaunchPhase.UNIGNITED);
				this.updateControllerSettings(pad);
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ISound setSoundUpdater(EntityPlayerSP player) {
		this.rocketSoundUpdater = new SoundUpdaterSatelliteRocket(player, this);
		return (ISound) this.rocketSoundUpdater;
	}

	public void setWaitForPlayer(boolean waitForPlayer) {
		this.waitForPlayer = waitForPlayer;
	}

	protected boolean shouldCancelExplosion() {
		return this.hasValidFuel();
	}

	@Override
	protected boolean shouldMoveClientSide() {
		return true;
	}

	public void stopRocketSound() {
		if (this.rocketSoundUpdater != null) {
			((SoundUpdaterRocket) this.rocketSoundUpdater).stopRocketSound();
		}
		this.rocketSoundToStop = false;
	}

	public void updateControllerSettings(IFuelDock dock) {
		HashSet<ILandingPadAttachable> connectedTiles = dock.getConnectedTiles();
		try {
			for (ILandingPadAttachable updatedTile : connectedTiles) {
				if (controllerClass.isInstance(updatedTile)) {
					// This includes a check for whether it has enough energy to run (if it doesn't, then a launch would not go to the destination frequency and the rocket would be lost!)
					this.activeLaunchController = new BlockVec3((TileEntity) updatedTile);
					if (controllerClass.getField("launchEnabled").getBoolean(updatedTile)) {
						this.autoLaunchSetting = EnumAutoLaunch.values()[controllerClass.getField("launchDropdownSelection").getInt(updatedTile)];

						switch (this.autoLaunchSetting) {
						case INSTANT:
							// Small countdown to give player a moment to exit the Launch Controller GUI
							if (this.autoLaunchCountdown <= 0 || this.autoLaunchCountdown > 12)
								this.autoLaunchCountdown = 12;
							break;
						// The other settings set time to count down AFTER player mounts rocket but BEFORE engine ignition
						// TODO: if autoLaunchCountdown > 0 add some smoke (but not flame) particle effects or other pre-flight test feedback so the player knows something is happening
						case TIME_10_SECONDS:
							if (this.autoLaunchCountdown <= 0 || this.autoLaunchCountdown > 200)
								this.autoLaunchCountdown = 200;
							break;
						case TIME_30_SECONDS:
							if (this.autoLaunchCountdown <= 0 || this.autoLaunchCountdown > 600)
								this.autoLaunchCountdown = 600;
							break;
						case TIME_1_MINUTE:
							if (this.autoLaunchCountdown <= 0 || this.autoLaunchCountdown > 1200)
								this.autoLaunchCountdown = 1200;
							break;
						default:
							break;
						}
					} else {
						// This LaunchController is out of power, disabled, invalid target or set not to launch
						// No auto launch - but maybe another connectedTile will have some launch settings?
						this.autoLaunchSetting = null;
						this.autoLaunchCountdown = 0;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);

		if (this.fuelTank.getFluid() != null) {
			nbt.setTag("fuelTank", this.fuelTank.writeToNBT(new NBTTagCompound()));
		}

		if (this.getSizeInventory() > 0) {
			ItemStackHelper.saveAllItems(nbt, this.stacks);
		}

		nbt.setBoolean("WaitingForPlayer", this.getWaitForPlayer());
		nbt.setBoolean("Landing", this.launchPhase == EnumLaunchPhase.LANDING.ordinal());
		nbt.setInteger("AutoLaunchSetting", this.autoLaunchSetting != null ? this.autoLaunchSetting.getIndex() : -1);
		nbt.setInteger("TimeUntilAutoLaunch", this.autoLaunchCountdown);
		if (this.activeLaunchController != null)
			this.activeLaunchController.writeToNBT(nbt, "ALCat");
	}
}
