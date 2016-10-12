package drzhark.mocreatures.item;

import com.mojang.authlib.GameProfile;

import drzhark.mocreatures.MoCPetData;
import drzhark.mocreatures.MoCTools;
import drzhark.mocreatures.MoCreatures;
import drzhark.mocreatures.entity.MoCEntityTameableAnimal;
import drzhark.mocreatures.entity.passive.MoCEntityHorse;
import drzhark.mocreatures.entity.passive.MoCEntityWyvern;
import drzhark.mocreatures.network.MoCMessageHandler;
import drzhark.mocreatures.network.message.MoCMessageAppear;
import drzhark.mocreatures.util.MoCSoundEvents;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.UUID;

public class MoCItemHorseAmulet extends MoCItem {

    private int ageCounter;
    private String name;
    private float health;
    private int edad;
    private int creatureType;
    private int spawnClass;
    private boolean rideable;
    private byte armor;
    private boolean adult;
    private UUID ownerUniqueId;
    private int PetId;

    public MoCItemHorseAmulet(String name) {
        super(name);
        this.maxStackSize = 1;
        setHasSubtypes(true);
        this.ageCounter = 0;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World worldIn, EntityPlayer player, EnumHand hand) {
        if (++this.ageCounter < 2) {
            return new ActionResult<ItemStack>(EnumActionResult.PASS, stack);
        }

        //int i = itemstack.getItemDamage();

        if (MoCreatures.isServer()) {
            initAndReadNBT(stack);
        }

        /*if (this.spawnClass == 21 || this.spawnClass == 0) // horses or old amulets
        {
            //dirty fix for old amulets
            this.spawnClass = 22;
            if (this.spawnClass == 0 || this.creatureType == 0) {
                this.creatureType = i;
                this.spawnClass = 22;
                this.edad = 100;
                this.health = 20;
                this.armor = 0;
                this.name = "";
                this.ownerName = "";
                this.rideable = false;
                this.adult = true;
            }
        }*/

        if (this.spawnClass != 0) {//(i != 0) {

            double dist = 3D;
            double newPosY = player.posY;
            double newPosX = player.posX - (dist * Math.cos((MoCTools.realAngle(player.rotationYaw - 90F)) / 57.29578F));
            double newPosZ = player.posZ - (dist * Math.sin((MoCTools.realAngle(player.rotationYaw - 90F)) / 57.29578F));

            if (MoCreatures.isServer()) {
                try {
                    MoCEntityTameableAnimal storedCreature;
                    if (this.spawnClass == 100) { //ghost wyvern
                        storedCreature = new MoCEntityWyvern(worldIn);
                    } else {

                        storedCreature = new MoCEntityHorse(worldIn);
                    }

                    storedCreature.setPosition(newPosX, newPosY, newPosZ);
                    storedCreature.setType(this.creatureType);
                    storedCreature.setTamed(true);
                    storedCreature.setRideable(this.rideable);
                    storedCreature.setEdad(this.edad);
                    storedCreature.setPetName(this.name);
                    storedCreature.setHealth(this.health);
                    storedCreature.setAdult(this.adult);
                    storedCreature.setArmorType(this.armor);
                    storedCreature.setOwnerPetId(this.PetId);
                    storedCreature.setOwnerId(player.getUniqueID());
                    if (this.spawnClass == 100) {
                        ((MoCEntityWyvern) storedCreature).setIsGhost(true);
                    }

                    //if the player using the amulet is different than the original owner
                    if (!(this.ownerUniqueId.equals(player.getUniqueID())) && MoCreatures.instance.mapData != null) {
                        MoCPetData oldOwner = MoCreatures.instance.mapData.getPetData(this.ownerUniqueId);
                        MoCPetData newOwner = MoCreatures.instance.mapData.getPetData(player.getUniqueID());
                        EntityPlayer epOwner = worldIn.getPlayerEntityByName(player.getName());
                        int maxCount = MoCreatures.proxy.maxTamed;
                        if (MoCTools.isThisPlayerAnOP(epOwner)) {
                            maxCount = MoCreatures.proxy.maxOPTamed;
                        }
                        if (newOwner == null) {
                            if (maxCount > 0 || !MoCreatures.proxy.enableOwnership) {
                                // create new PetData for new owner
                                MoCreatures.instance.mapData.updateOwnerPet(storedCreature);
                            }
                        } else // add pet to existing pet data
                        {
                            if (newOwner.getTamedList().tagCount() < maxCount || !MoCreatures.proxy.enableOwnership) {
                                MoCreatures.instance.mapData.updateOwnerPet(storedCreature);
                            }
                        }
                        // remove pet entry from old owner
                        if (oldOwner != null) {
                            for (int j = 0; j < oldOwner.getTamedList().tagCount(); j++) {
                                NBTTagCompound petEntry = oldOwner.getTamedList().getCompoundTagAt(j);
                                if (petEntry.getInteger("PetId") == this.PetId) {
                                    // found match, remove
                                    oldOwner.getTamedList().removeTag(j);
                                }
                            }
                        }
                    }

                    if (player.worldObj.spawnEntityInWorld(storedCreature)) {
                        MoCMessageHandler.INSTANCE.sendToAllAround(new MoCMessageAppear(storedCreature.getEntityId()), new TargetPoint(
                                player.worldObj.provider.getDimensionType().getId(), player.posX, player.posY, player.posZ, 64));
                        MoCTools.playCustomSound(storedCreature, MoCSoundEvents.ENTITY_GENERIC_MAGIC_APPEAR);
                        //gives an empty amulet
                        if (this.spawnClass == 100 || this.creatureType == 21 || this.creatureType == 22) {
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(
                                    MoCreatures.amuletghost, 1, 0));
                        } else if (this.creatureType == 26 || this.creatureType == 27 || this.creatureType == 28) {
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(MoCreatures.amuletbone,
                                    1, 0));
                        } else if ((this.creatureType > 47 && this.creatureType < 60)) {
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(
                                    MoCreatures.amuletfairy, 1, 0));
                        } else if (this.creatureType == 39 || this.creatureType == 40) {
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(
                                    MoCreatures.amuletpegasus, 1, 0));
                        }
                        MoCPetData petData = MoCreatures.instance.mapData.getPetData(storedCreature.getOwnerId());
                        if (petData != null) {
                            petData.setInAmulet(storedCreature.getOwnerPetId(), false);
                        }
                    }
                } catch (Exception ex) {
                    System.out.println("Error spawning creature from amulet " + ex);
                }
            }
            this.ageCounter = 0;
        }

        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }

    public void readFromNBT(NBTTagCompound nbt) {
        this.PetId = nbt.getInteger("PetId");
        this.creatureType = nbt.getInteger("CreatureType");
        this.health = nbt.getFloat("Health");
        this.edad = nbt.getInteger("Edad");
        this.name = nbt.getString("Name");
        this.spawnClass = nbt.getInteger("SpawnClass");
        this.rideable = nbt.getBoolean("Rideable");
        this.armor = nbt.getByte("Armor");
        this.adult = nbt.getBoolean("Adult");
        this.ownerUniqueId = nbt.getUniqueId("OwnerUUID");
    }

    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("PetID", this.PetId);
        nbt.setInteger("CreatureType", this.creatureType);
        nbt.setFloat("Health", this.health);
        nbt.setInteger("Edad", this.edad);
        nbt.setString("Name", this.name);
        nbt.setInteger("SpawnClass", this.spawnClass);
        nbt.setBoolean("Rideable", this.rideable);
        nbt.setByte("Armor", this.armor);
        nbt.setBoolean("Adult", this.adult);
        nbt.setUniqueId("OwnerUUID", this.ownerUniqueId);
    }

    @SideOnly(Side.CLIENT)
    /**
     * allows items to add custom lines of information to the mouseover description
     */
    @Override
    public void addInformation(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List<String> par3List, boolean par4) {
        initAndReadNBT(par1ItemStack);
        if (this.spawnClass == 100) {
            par3List.add(TextFormatting.AQUA + "Wyvern");
        } else if (this.spawnClass != 0) {
            par3List.add(TextFormatting.AQUA + "WildHorse");
        }
        if (this.name != "") {
            par3List.add(TextFormatting.BLUE + this.name);
        }
        if (this.ownerUniqueId != null) {
        	try{
        		GameProfile profile = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerProfileCache().getProfileByUUID(this.ownerUniqueId);
                if (profile != null) {
                    par3List.add(TextFormatting.DARK_BLUE + "Owned by " + profile.getName());
                }	
        	} catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initAndReadNBT(ItemStack itemstack) {
        if (itemstack.getTagCompound() == null) {
            itemstack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbtcompound = itemstack.getTagCompound();
        readFromNBT(nbtcompound);
    }
}
