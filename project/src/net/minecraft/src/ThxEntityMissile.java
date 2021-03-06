package net.minecraft.src;


public class ThxEntityMissile extends ThxEntityProjectile
{
	public ThxEntityMissile(World world)
    {
        super(world);
    }
	
    public ThxEntityMissile(World world, double x, double y, double z)
    {
        super(world, x, y, z);
    }

    public ThxEntityMissile(Entity owner, double x, double y, double z, double dx, double dy, double dz, float yaw, float pitch)
    {
        super(owner, x, y, z, dx, dy, dz, yaw, pitch);
    }

    @Override
    ThxEntityHelper createEntityHelper()
    {
        log("createHelper()");
        return new ThxEntityHelperClient(this, new ThxModelMissile());
    }
    
    @Override
    void createParticles()
    {
        worldObj.spawnParticle("smoke", posX, posY, posZ, 0.0, 0.0, 0.0);
        worldObj.spawnParticle("largesmoke", posX, posY, posZ, 0.0, 0.0, 0.0);
    }
    
    @Override
    void onLaunch()
    {
        System.out.println("Missile launch: " + this);
        worldObj.playSoundAtEntity(this, "mob.irongolem.throw", 1f, 1f);
        
        super.onLaunch();
    }
    
    @Override
    float getAcceleration()
    {
        //return .52f;
        return .5f; // very small for testing
    }
    
    @Override
    void strikeEntity(Entity entity)
    {
        if (worldObj.isRemote) return; // only applies on server
        
        int attackStrength = 12; // about 2 hearts for other player without armor??;
        entity.attackEntityFrom(new EntityDamageSource("player", owner), attackStrength);
        
        entity.setFire(10); // direct hit, burn for 10 seconds
    }
    
    @Override
    void detonate()
    {
        if (worldObj.isRemote) return; // only applies on server
        
        doSplashDamage(3.0, 6); // 1 heart if within range, see explosion impl for more precise way
            
        //float power = 1.1f; // v018 power to break dirt, sand, wood only and harvest items
        float power = 1.4f; // break a single stone block?
        
        boolean withFire = false;
        boolean smoking = true; // oddly, this flag triggers block interactions
        worldObj.newExplosion(this, posX, posY, posZ, power, withFire, smoking);
        
        setDead();
    }
}

