package net.minecraft.src;

import java.util.List;

public abstract class ThxEntity extends Entity
{
    boolean plog = true; // enable periodic logging for rapidly repeating events

    final float RAD_PER_DEG = 00.01745329f;
    final float PI = 03.14159265f;

    long prevTime;
    float deltaTime;

    double prevMotionX;
    double prevMotionY;
    double prevMotionZ;

    float rotationRoll;
    float prevRotationRoll;

    float yawRad;
    float pitchRad;
    float rollRad;

    float rotationYawSpeed;
    float rotationPitchSpeed;
    float rotationRollSpeed;

    Vector3 pos;
    Vector3 vel;
    Vector3 ypr;

    Vector3 fwd;
    Vector3 side;
    Vector3 up;
    
    ThxEntityHelper helper;
    Packet230ModLoader latestUpdatePacket;
    int NET_PACKET_TYPE;
    
    int fire1;
    int fire2;
    int cmd_exit;
    int cmd_create_map;
    
    float damage;
    float throttle;
    
    Entity owner;
    
    // total update count
    float timeSinceAttacked;
    float timeSinceCollided;
    
    public ThxEntity(World world)
    {
        super(world);

        log(world.isRemote ? "Created new MP entity" : "Created new SP entity");

        preventEntitySpawning = true;

        // vectors relative to entity orientation
        fwd = new Vector3();
        side = new Vector3();
        up = new Vector3();

        pos = new Vector3();
        vel = new Vector3();
        ypr = new Vector3();

        prevTime = System.nanoTime();
    }

    @Override
    public void onUpdate()
    {
        ticksExisted++;
        int riddenById = riddenByEntity != null ? riddenByEntity.entityId : 0;
        plog(String.format("start onUpdate, pilot %d [posX: %5.2f, posY: %5.2f, posZ: %5.2f, yaw: %5.2f]", riddenById, posX, posY, posZ, rotationYaw));
        
        long time = System.nanoTime();
        deltaTime = ((float) (time - prevTime)) / 1000000000f; // convert to sec
        prevTime = time;

        lastTickPosX = prevPosX = posX;
        lastTickPosY = prevPosY = posY;
        lastTickPosZ = prevPosZ = posZ;

        prevRotationPitch = rotationPitch;
        prevRotationYaw = rotationYaw;
        prevRotationRoll = rotationRoll;
        
        inWater = isInWater();
        
        // decrement cooldown timers
        timeSinceAttacked -= deltaTime;
        timeSinceCollided -= deltaTime;
        
        updateRotation();
        updateVectors();
    }
    
    public boolean isInWater()
    {
        // check for contact with water
        return worldObj.isAABBInMaterial(boundingBox.expand(.0, -.4, .0), Material.water);
    }
    
    /*
     * Normalize all rotations to -180 to +180 degrees (typically only yaw is * affected)
     */
    public void updateRotation()
    {
        rotationYaw %= 360f;
        if (rotationYaw > 180f) rotationYaw -= 360f;
        else if (rotationYaw < -180f) rotationYaw += 360f;
        yawRad = rotationYaw * RAD_PER_DEG;

        rotationPitch %= 360f;
        if (rotationPitch > 180f) rotationPitch -= 360f;
        else if (rotationPitch < -180f) rotationPitch += 360f;
        pitchRad = rotationPitch * RAD_PER_DEG;

        rotationRoll %= 360f;
        if (rotationRoll > 180f) rotationRoll -= 360f;
        else if (rotationRoll < -180f) rotationRoll += 360f;
        rollRad = rotationRoll * RAD_PER_DEG;
    }

    public void updateVectors()
    {
        float cosYaw   = (float) MathHelper.cos(-yawRad - PI);
        float sinYaw   = (float) MathHelper.sin(-yawRad - PI);
        float cosPitch = (float) MathHelper.cos(-pitchRad);
        float sinPitch = (float) MathHelper.sin(-pitchRad);
        float cosRoll  = (float) MathHelper.cos(-rollRad);
        float sinRoll  = (float) MathHelper.sin(-rollRad);

        fwd.x = -sinYaw * cosPitch;
        fwd.y = sinPitch;
        fwd.z = -cosYaw * cosPitch;

        side.x = cosYaw * cosRoll;
        side.y = -sinRoll;
        side.z = -sinYaw * cosRoll;

        // up.x = cosYaw * sinRoll - sinYaw * sinPitch * cosRoll;
        // up.y = cosPitch * cosRoll;
        // up.z = -sinYaw * sinRoll - sinPitch * cosRoll * cosYaw;
        Vector3.cross(side, fwd, up);

        pos.x = (float) posX;
        pos.y = (float) posY;
        pos.z = (float) posZ;

        vel.x = (float) motionX;
        vel.y = (float) motionY;
        vel.z = (float) motionZ;

        ypr.x = rotationYaw;
        ypr.y = rotationPitch;
        ypr.z = rotationRoll;
    }

    public Vector3 getForward()
    {
        float f3 = MathHelper.sin(-rotationYaw * 0.01745329F - 3.141593F);
        float f1 = MathHelper.cos(-rotationYaw * 0.01745329F - 3.141593F);
        float f5 = -MathHelper.cos(-rotationPitch * 0.01745329F);
        float f7 = MathHelper.sin(-rotationPitch * 0.01745329F);
        return new Vector3(f3 * f5, f7, f1 * f5);
    }

    @Override
    public void setEntityDead()
    {
        log("setEntityDead called");
        super.setEntityDead();
    }

    @Override
    protected void fall(float f)
    {
        // no damage from falling, unlike super.fall
        log("fall() called with arg " + f);
    }
    
    /* abstract methods from Entity base class */
    @Override
    protected void readEntityFromNBT(NBTTagCompound nbttagcompound)
    {
        //log("readEntityFromNBT called");
    }
    
    @Override
    protected void writeEntityToNBT(NBTTagCompound nbttagcompound)
    {
        //log("writeEntityToNBT called");
    }
    
    @Override
    public String toString()
    {
        return this.getClass().getSimpleName() + " " + entityId;
    }

    void log(String s)
    {
        mod_Thx.log(String.format("[%5d] ", ticksExisted) + this + ": " + s);
    }
    
    void plog(String s) // periodic log
    {
        if (plog && ticksExisted % 60 == 0)
        {
            log(s);
        }
    }
    
    public Packet230ModLoader getUpdatePacket()
    {
        Packet230ModLoader packet = new Packet230ModLoader();

        packet.modId = mod_Thx.instance.getId();
        packet.packetType = NET_PACKET_TYPE;

        packet.dataString = new String[] { "thx update packet for tick " + ticksExisted };

        packet.dataInt = new int[7];
        packet.dataInt[0] = entityId;
        packet.dataInt[1] = riddenByEntity != null ? riddenByEntity.entityId : 0;
        packet.dataInt[2] = fire1;
        packet.dataInt[3] = fire2;
        packet.dataInt[4] = owner != null ? owner.entityId : 0;
        packet.dataInt[5] = cmd_exit;
        packet.dataInt[6] = cmd_create_map;
        
        // clear cmd flags after setting them in packet
        fire1 = 0;
        fire2 = 0;
        cmd_exit = 0;
		cmd_create_map = 0;
		
        packet.dataFloat = new float[11];
        packet.dataFloat[0] = (float) posX;
        packet.dataFloat[1] = (float) posY;
        packet.dataFloat[2] = (float) posZ;
        packet.dataFloat[3] = rotationYaw;
        packet.dataFloat[4] = rotationPitch;
        packet.dataFloat[5] = rotationRoll;
        packet.dataFloat[6] = (float) motionX;
        packet.dataFloat[7] = (float) motionY;
        packet.dataFloat[8] = (float) motionZ;
        packet.dataFloat[9] = damage;
        packet.dataFloat[10] = throttle;
        
        return packet;
    }
    
    @Override
    public boolean interact(EntityPlayer player)
    {
        log("interact called with player " + player.entityId);
        
        if (player.equals(riddenByEntity))
        {
            if (onGround || isCollidedVertically)
            {
                log("Landed and exited");
            }
            else 
            {
                log("Exited without landing");
            }
            //pilotExit();
            //return true;
            return false;
        }
        
        if (player.ridingEntity != null) 
        {
            // player is already riding some other entity
            return false;
        }
        
        if (riddenByEntity != null)
        {
            // already ridden by some other entity, allow takeover if close
            if (getDistanceSqToEntity(player) < 3.0)
            {
                log("current pilot was ejected");
                pilotExit();
            }
            else
            {
	            return false;
            }
        }
        
        // new pilot boarding
        if (!worldObj.isRemote)
        {
	        log("interact() calling mountEntity on player " + player.entityId);
	        player.mountEntity(this);
        }
        
        player.rotationYaw = rotationYaw;
        updateRiderPosition();
        
        log("interact() added pilot: " + player);
        return true;
    }
    
    public int getPilotId()
    {
        return riddenByEntity != null ? riddenByEntity.entityId : 0;
    }
    
    protected void pilotExit()
    {
        log("pilotExit called for pilot " + riddenByEntity + " " + getPilotId());
    }
    
    void takeDamage(float amount)
    {
        damage += amount;
    }
    
    @Override
    public boolean attackEntityFrom(DamageSource damageSource, int i)
    {
        log("attackEntityFrom called with damageSource: " + damageSource);

        if (timeSinceAttacked > 0f || isDead || damageSource == null) return false;

        Entity attackingEntity = damageSource.getEntity();
        if (attackingEntity == null) return false; // when is this the case?
        if (attackingEntity.equals(this)) return false; // ignore damage from self
        if (attackingEntity.equals(riddenByEntity))
        {
            //riddenByEntity.mountEntity(this);
            pilotExit();
            setEntityDead();
            if (this instanceof ThxEntityHelicopter && !worldObj.isRemote)
            {
                // could add a dropItem() method for subclasses instead of instanceof
                dropItemWithOffset(ThxItemHelicopter.shiftedId, 1, 0);
            }
            return false; // ignore damage from pilot
        }
        log("attacked by entity: " + attackingEntity);
        return true; // hit landed
    }
    
    @Override
    public boolean canBeCollidedWith()
    {
        return !isDead;
    }

    @Override
    public boolean canBePushed()
    {
        return true;
    }

    @Override
    protected boolean canTriggerWalking()
    {
        return false;
    }

    @Override
    protected void entityInit()
    {
        log(this + " entityInit called");
    }

    @Override
    public AxisAlignedBB getBoundingBox()
    {
        return boundingBox;
    }

    @Override
    public AxisAlignedBB getCollisionBox(Entity entity)
    {
        return entity.boundingBox;
    }

    public boolean isInRangeToRenderDist(double d)
    {
        return d < 128.0 * 128.0;
    }

}
