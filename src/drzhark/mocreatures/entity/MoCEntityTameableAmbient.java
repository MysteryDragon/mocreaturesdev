package drzhark.mocreatures.entity;

import java.util.List;
import java.util.Map;

import drzhark.mocreatures.MoCPetData;
import drzhark.mocreatures.MoCTools;
import drzhark.mocreatures.MoCreatures;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

public abstract class MoCEntityTameableAmbient extends MoCEntityAmbient implements IMoCTameable
{
    public MoCEntityTameableAmbient(World world)
    {
        super(world);
    }

    @Override
    protected void entityInit()
    {
        super.entityInit();
        dataWatcher.addObject(30, -1); // PetId    
    }

    public int getOwnerPetId()
    {
        return dataWatcher.getWatchableObjectInt(30);
    }

    public void setOwnerPetId(int i)
    {
        dataWatcher.updateObject(30, i);
    }

    public boolean interact(EntityPlayer entityplayer)
    {
        ItemStack itemstack = entityplayer.inventory.getCurrentItem();
        //before ownership check 
        if ((itemstack != null) && getIsTamed() && ((itemstack.itemID == MoCreatures.scrollOfOwner.itemID)) 
                && MoCreatures.proxy.enableResetOwnership && MoCTools.isThisPlayerAnOP(entityplayer))
        {
            if (--itemstack.stackSize == 0)
            {
                entityplayer.inventory.setInventorySlotContents(entityplayer.inventory.currentItem, null);
            }
            if (MoCreatures.isServer())
            {
                if (this.getOwnerPetId() != -1) // required since getInteger will always return 0 if no key is found
                {
                    MoCreatures.instance.mapData.removeOwnerPet(this, this.getOwnerPetId());//this.getOwnerPetId());
                }
                this.setOwner("");
                
            }
            return true;
        }
        //if the player interacting is not the owner, do nothing!
        if (MoCreatures.proxy.enableOwnership && getOwnerName() != null && !getOwnerName().equals("") && !entityplayer.username.equals(getOwnerName())) 
        {
            return true; 
        }

        //changes name
        if ((itemstack != null) && getIsTamed() //&& MoCreatures.isServer()
                && ((itemstack.itemID == MoCreatures.medallion.itemID) || (itemstack.itemID == Item.book.itemID)))
        {
            if (MoCreatures.isServer())
            {
                MoCTools.tameWithName((EntityPlayerMP) entityplayer, this);
            }
            return true;
        }
        
        //sets it free, untamed
        if ((itemstack != null) && getIsTamed() 
                && ((itemstack.itemID == MoCreatures.scrollFreedom.itemID)))
        {
            if (--itemstack.stackSize == 0)
            {
                entityplayer.inventory.setInventorySlotContents(entityplayer.inventory.currentItem, null);
            }
            if (MoCreatures.isServer())
            {
                if (this.getOwnerPetId() != -1) // required since getInteger will always return 0 if no key is found
                {
                    MoCreatures.instance.mapData.removeOwnerPet(this, this.getOwnerPetId());//this.getOwnerPetId());
                }
                this.setOwner("");
                this.setName("");
                this.dropMyStuff();
                this.setTamed(false);
            }

            return true;
        }

        //removes owner, any other player can claim it by renaming it
        if ((itemstack != null) && getIsTamed() 
                    && ((itemstack.itemID == MoCreatures.scrollOfSale.itemID)))
        {
            if (--itemstack.stackSize == 0)
            {
                entityplayer.inventory.setInventorySlotContents(entityplayer.inventory.currentItem, null);
            }
            if (MoCreatures.isServer())
            {
                if (this.getOwnerPetId() != -1) // required since getInteger will always return 0 if no key is found
                {
                    MoCreatures.instance.mapData.removeOwnerPet(this, this.getOwnerPetId());//this.getOwnerPetId());
                }
                this.setOwner("");
            }
            return true;
        }

        if ((itemstack != null) && getIsTamed() && isMyHealFood(itemstack))
        {
            if (--itemstack.stackSize == 0)
            {
                entityplayer.inventory.setInventorySlotContents(entityplayer.inventory.currentItem, null);
            }
            worldObj.playSoundAtEntity(this, "eating", 1.0F, 1.0F + ((rand.nextFloat() - rand.nextFloat()) * 0.2F));
            if (MoCreatures.isServer())
            {
                health = getMaxHealth();
            }
            return true;
        }
        
        //stores in fishnet
        if (itemstack != null && itemstack.itemID == MoCreatures.fishnet.itemID && this.canBeTrappedInNet()) 
        {
            entityplayer.inventory.setInventorySlotContents(entityplayer.inventory.currentItem, null);
            if (MoCreatures.isServer())
            {
                MoCTools.dropAmulet((IMoCTameable) this, 1);
                this.isDead = true;
            }

            return true;
        }
        
        
      //heals
        if ((itemstack != null) && getIsTamed() && isMyHealFood(itemstack))
        {
            if (--itemstack.stackSize == 0)
            {
                entityplayer.inventory.setInventorySlotContents(entityplayer.inventory.currentItem, null);
            }
            worldObj.playSoundAtEntity(this, "eating", 1.0F, 1.0F + ((rand.nextFloat() - rand.nextFloat()) * 0.2F));
            if (MoCreatures.isServer())
            {
                health = getMaxHealth();
            }
            return true;
        }

        if ((itemstack != null) && getIsTamed() && (itemstack.itemID == Item.shears.itemID))
        {
            if (MoCreatures.isServer())
            {
                dropMyStuff();
            }
            
            return true;
        }

       
        return super.interact(entityplayer);
    }
    /**
     * Play the taming effect, will either be hearts or smoke depending on status
     */
    public void playTameEffect(boolean par1)
    {
        String s = "heart";

        if (!par1)
        {
            s = "smoke";
        }

        for (int i = 0; i < 7; ++i)
        {
            double d0 = this.rand.nextGaussian() * 0.02D;
            double d1 = this.rand.nextGaussian() * 0.02D;
            double d2 = this.rand.nextGaussian() * 0.02D;
            this.worldObj.spawnParticle(s, this.posX + (double)(this.rand.nextFloat() * this.width * 2.0F) - (double)this.width, this.posY + 0.5D + (double)(this.rand.nextFloat() * this.height), this.posZ + (double)(this.rand.nextFloat() * this.width * 2.0F) - (double)this.width, d0, d1, d2);
        }
    }
    
    
    @Override
    public void writeEntityToNBT(NBTTagCompound nbttagcompound)
    {
        super.writeEntityToNBT(nbttagcompound);
        if (getOwnerPetId() != -1)
            nbttagcompound.setInteger("PetId", this.getOwnerPetId());
        if (this instanceof IMoCTameable && getIsTamed())
        {
            MoCreatures.instance.mapData.updateOwnerPet((IMoCTameable)this, nbttagcompound);
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbttagcompound)
    {
        super.readEntityFromNBT(nbttagcompound);
        if (nbttagcompound.hasKey("PetId"))
            setOwnerPetId(nbttagcompound.getInteger("PetId"));
        if (this.getIsTamed() && nbttagcompound.hasKey("PetId"))
        {
            System.out.println("PET ID = " + nbttagcompound.getInteger("PetId"));
            MoCPetData petData = MoCreatures.instance.mapData.getPetData(this.getOwnerName());
            NBTTagList tag = petData.getPetData().getTagList("TamedList");
            for (int i = 0; i < tag.tagCount(); i++)
            {
                System.out.println("found tag " + tag.tagAt(i));
                NBTTagCompound nbt = (NBTTagCompound)tag.tagAt(i);
                if (nbt.getInteger("PetId") == nbttagcompound.getInteger("PetId"))
                {
                    // check if cloned and if so kill
                    if (nbt.hasKey("Cloned"))
                    {
                        // entity was cloned
                        System.out.println("CLONED!!, killing self");
                        nbt.removeTag("Cloned"); // clear flag
                        this.setTamed(false);
                        this.setDead();
                    }
                }
            }
        }
    }
}