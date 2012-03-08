package net.minecraft.src;

public class ThxEntityMissile extends ThxEntityMissileBase implements ISpawnable
{
    public ThxEntityMissile(World world)
    {
        super(world);
        System.out.println("C1 ThxEntityMissile called");
    }

    public ThxEntityMissile(World world, double x, double y, double z, double dx, double dy, double dz, float yaw, float pitch)
    {
        super(world, x, y, z, dx, dy, dz, yaw, pitch);
        System.out.println("C2 ThxEntityMissile called");
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();
    }
    
    /* from ISpawnable interface */
    @Override
    public Packet230ModLoader getSpawnPacket()
    {
        return helper.getSpawnPacket();
    }
    
    @Override
    ThxEntityHelper createHelper()
    {
	    return new ThxEntityHelperServer(this);
    }
}
