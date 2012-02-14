package net.minecraft.src;


public class ThxEntity extends Entity implements ISpawnable
{
    final float RAD_PER_DEG = 00.01745329f;
    final float PI          = 03.14159265f;

    Packet230ModLoader latestUpdatePacket;
    
    boolean plog = true;

    boolean visible = true;
    
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

    String renderTexture;
    
    Vector3 pos;
    Vector3 vel;
    Vector3 ypr;
    
    Vector3 fwd;
    Vector3 side;
    Vector3 up;
    
    boolean inWater;
    
    float plogDelay;
   
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

        //super.onUpdate();

        long time = System.nanoTime();
        deltaTime = ((float)(time - prevTime)) / 1000000000f; // convert to sec
        prevTime = time;
        
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        
        prevRotationPitch = rotationPitch;
        prevRotationYaw   = rotationYaw;
        prevRotationRoll  = rotationRoll;
	        
        applyUpdatePacketFromClient();    
        
        updateRotation();
        updateVectors();
        
        // check for water
        inWater = worldObj.isAABBInMaterial(boundingBox.expand(.0, -.4, .0), Material.water);
    }
 
    /*
     *  Normalize all rotations to -180 to +180 degrees (typically only yaw is affected)
     */
    private void updateRotation()
    {
        rotationYaw   %= 360f;
        if (rotationYaw > 180f) rotationYaw -= 360f;
        else if (rotationYaw < -180f) rotationYaw += 360f;
        yawRad = rotationYaw * RAD_PER_DEG;
        
        rotationPitch %= 360f;
        if (rotationPitch > 180f) rotationPitch -= 360f;
        else if (rotationPitch < -180f) rotationPitch += 360f;
        pitchRad = rotationPitch * RAD_PER_DEG;
        
        rotationRoll  %= 360f;
        if (rotationRoll > 180f) rotationRoll -= 360f;
        else if (rotationRoll < -180f) rotationRoll += 360f;
        rollRad = rotationRoll * RAD_PER_DEG;
        
        //plog("rotationYaw: " + rotationYaw + ", rotationPitch: " + rotationPitch + ", rotationRoll: " + rotationRoll);
    }
    
    private void updateVectors()
    {
        float cosRoll  = (float) MathHelper.cos(-rollRad);
        float sinRoll  = (float) MathHelper.sin(-rollRad);
        float cosYaw   = (float) MathHelper.cos(-yawRad - PI);
        float sinYaw   = (float) MathHelper.sin(-yawRad - PI);
        float cosPitch = (float) MathHelper.cos(-pitchRad);
        float sinPitch = (float) MathHelper.sin(-pitchRad);
        
        fwd.x = -sinYaw * cosPitch;
        fwd.y = sinPitch;
        fwd.z = -cosYaw * cosPitch;
        
        side.x = cosYaw * cosRoll;
        side.y = -sinRoll;
        side.z = -sinYaw * cosRoll;
        
        //up.x = cosYaw * sinRoll - sinYaw * sinPitch * cosRoll; 
        //up.y = cosPitch * cosRoll;
        //up.z = -sinYaw * sinRoll - sinPitch * cosRoll * cosYaw;
        Vector3.cross(fwd, side, up);
        
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

    void plog(String s) // periodic log
    {
        if (plog && ticksExisted % 60 == 0)
        {
            log(s);
        }
    }
    
    void log(String s)
    {        
        System.out.println("[" + ticksExisted + "] " + this + ": " + s);
    }
    
    @Override
    public String toString()
    {
        return this.getClass().getSimpleName() + " " + entityId;
    }

    /* abstract methods from Entity base class */
    @Override
    protected void entityInit()
    {
        log("entityInit called");
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbttagcompound)
    {
        log("readEntityFromNBT called");
    }
    
    @Override
    protected void writeEntityToNBT(NBTTagCompound nbttagcompound)
    {
        log("writeEntityToNBT called");
    }
    
    /** ISpawnable server interface */
    @Override
    public Packet230ModLoader getSpawnPacket()
    {
        Packet230ModLoader packet = getUpdatePacket();
        packet.dataString[0] = "thx spawn packet for entity " + entityId;
        log("Created new spawn packet: " + packet);
        return packet;
    }
        
    public Packet230ModLoader getUpdatePacket()
    {
        log("getUpdatePacket()");
        
        Packet230ModLoader packet = new Packet230ModLoader();
        
        packet.modId = mod_Thx.instance.getId();
        packet.packetType = 75; // entity.entityNetId;
        
        packet.dataString = new String[]{ "thx update packet for tick " + ticksExisted };
        
        packet.dataInt = new int[2];
        packet.dataInt[0] = entityId;
        packet.dataInt[1] = riddenByEntity != null ? riddenByEntity.entityId : 0;
        
        packet.dataFloat = new float[9];
        packet.dataFloat[0] = (float) posX;
        packet.dataFloat[1] = (float) posY;
        packet.dataFloat[2] = (float) posZ;
        packet.dataFloat[3] = rotationYaw;
        packet.dataFloat[4] = rotationPitch;
        packet.dataFloat[5] = rotationRoll;
        packet.dataFloat[6] = (float) motionX;
        packet.dataFloat[7] = (float) motionY;
        packet.dataFloat[8] = (float) motionZ;
        
        return packet;
    }
    
    public void handleUpdatePacketFromClient(Packet230ModLoader packet)
    {
        if (packet.dataInt == null || packet.dataInt.length != 2)
        {
            log("Ignoring update packet without entity IDs");
            return;
        }
        if (entityId != packet.dataInt[0])
        {
            log("Ignoring update packet with wrong entity id " + packet.dataInt[0]);
            return;
        }
        if (riddenByEntity == null)
        {
            log("Ignoring update packet since we have no pilot");
            return;
        }
        if (riddenByEntity.entityId != packet.dataInt[1])
        {
            log("Ignoring update packet with wrong pilot id " + packet.dataInt[1]);
            return;
        }
        
        log("handleUpdatePacket - posX: " + packet.dataFloat[0] + ", posY: " + packet.dataFloat[1] + ", posZ: " + packet.dataFloat[2]);
        
        latestUpdatePacket = packet;
    }
    
    private void applyUpdatePacketFromClient()
    {
        plog("applyUpdatePacketFromClient(), latest packet: " + latestUpdatePacket);
        
        if (latestUpdatePacket == null) return;
        
        Packet230ModLoader p = latestUpdatePacket;
        //latestUpdatePacket = null; uncomment to only apply each packet once
        
        setPosition(p.dataFloat[0], p.dataFloat[1], p.dataFloat[2]);
        setRotation(p.dataFloat[3], p.dataFloat[4]);
        
        rotationRoll  = p.dataFloat[5] % 360f;
        
        // for now, clear any motion
        motionX       = .0; //p.dataFloat[6];
        motionY       = .0; //p.dataFloat[7];
        motionZ       = .0; //p.dataFloat[8];
        
        
        if (riddenByEntity == null)
        {
            System.out.println("update received even though no pilot");
            latestUpdatePacket = null;
        }
        else if (riddenByEntity.isDead)
        {
	        // last update for this player packet if player is dead
            log ("pilot entity is dead");
            riddenByEntity.mountEntity(this);
            latestUpdatePacket = null;
        }
        else 
        {
            updateRiderPosition();
        }
    }

}