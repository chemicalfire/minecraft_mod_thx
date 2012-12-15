package net.minecraft.src;

import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;

public class mod_Thx extends BaseMod
{
    static ThxConfig config;
    
    public static mod_Thx instance;

    static int HELICOPTER_TYPE_ID = 99;
    static int ROCKET_TYPE_ID     = 100;
    static int MISSILE_TYPE_ID    = 101;
    
    static WorldClient theWorld = null;
    
    public mod_Thx()
    {
        /* java.util.logging approach...
        try
        {
            Handler handler = new FileHandler("mods/mod_thx.log");
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
            
            String level = ThxConfig.getProperty("enable_logging_level", "SEVERE");
            System.out.println("thxLog.level: " + level);
            logger.setLevel(Level.parse(level));
        }
        catch (Exception e)
        {
            System.out.println("Could not open log file 'mods/mod_thx.log': " + e);
        }
        logger.fine("log fine test");
        logger.info("log info test");
        logger.warning("log warning test");
        */
        
        System.out.println("mod_Thx() called");
		config = new ThxConfig();
        instance = this; // for easy access by static methods and to instance methods
        
    }

    @Override
    public void load()
    {
        //log("load() called");

        ModLoader.setInGameHook(this, true, true);
        ModLoader.registerPacketChannel(this, "THX_entity");

        int drawDistance = 64; // typically 160, reduced for testing spawn/despawn
        int updateFreq = 1; // 20 for 1 second updates
        boolean trackMotion = true;
            
        // register entity classes
        helicopter:
        {
            int entityId = ModLoader.getUniqueEntityId();
            log("Registering entity class for Helicopter with ModLoader entity id " + entityId);
            ModLoader.registerEntityID(ThxEntityHelicopter.class, "thxHelicopter", entityId);
            ModLoader.addEntityTracker(this, ThxEntityHelicopter.class, HELICOPTER_TYPE_ID, drawDistance, updateFreq, trackMotion);
        }
        rocket:
        {
            int entityId = ModLoader.getUniqueEntityId();
            log("Registering entity class for Rocket with entity id " + entityId);
            ModLoader.registerEntityID(ThxEntityRocket.class, "thxRocket", entityId);
            ModLoader.addEntityTracker(this, ThxEntityRocket.class, ROCKET_TYPE_ID, drawDistance, updateFreq, trackMotion);
        }
        missile:
        {
            int entityId = ModLoader.getUniqueEntityId();
            log("Registering entity class for Missile with entity id " + entityId);
            ModLoader.registerEntityID(ThxEntityMissile.class, "thxMissile", entityId);
            ModLoader.addEntityTracker(this, ThxEntityMissile.class, MISSILE_TYPE_ID, drawDistance, updateFreq, trackMotion);
        }

        helicopterItem:
        {
            int itemId = getNextItemId();
            log("Setting up inventory item for helicopter with item id " + itemId);
            Item item = new ThxItemHelicopter(itemId);

            if (config.getBoolProperty("disable_helicopter_item_image"))
            {
                item.setIconIndex(92); // hard-code to cookie icon for compatibility
            }
            else
            {
                item.setIconIndex(ModLoader.addOverride("/gui/items.png", "/thx/helicopter_icon.png"));
            }
            item.setItemName("thxHelicopter");
            ModLoader.addName(item, "THX Helicopter Prototype");

            log("Adding recipe for helicopter");
            ItemStack itemStack = new ItemStack(item, 1, 1);
            Object[] recipe = new Object[] { " X ", "X X", "XXX", Character.valueOf('X'), Block.planks };
            ModLoader.addRecipe(itemStack, recipe);
        }

        log("Done loading " + getVersion());
    }

    @Override
    public void addRenderer(java.util.Map map)
    {
        map.put(ThxEntityHelicopter.class, new ThxRender());
        map.put(ThxEntityRocket.class, new ThxRender());
        map.put(ThxEntityMissile.class, new ThxRender());
    }

    @Override
    public String getVersion()
    {
        //log("getVersion called");
        return "Minecraft THX Helicopter Mod - mod_thx-mc145_v020";
    }

    int getNextItemId()
    {
        // return next available id
        for (int idx = 24000; idx + 256 < Item.itemsList.length; idx++)
        {
            if (Item.itemsList[idx + 256] == null) return idx;
        }
        // error:
        throw new RuntimeException("Could not find next available Item ID -- can't continue!");
    }

    static void log(String s)
    {
        if (!ThxConfig.ENABLE_LOGGING) return;
        
        if (theWorld == null) theWorld = ModLoader.getMinecraftInstance().theWorld;
        
        System.out.println(String.format("[%5d] mod_thx: ", theWorld != null ? theWorld.getWorldTime() : 0)  + s);
    }
    
    static void plog(String s) // periodic log
    {
        if (!ThxConfig.ENABLE_LOGGING) return;
        if (theWorld == null) theWorld = ModLoader.getMinecraftInstance().theWorld;
        if (theWorld != null && theWorld.getWorldTime() % 60 == 0)
        {
            log(s); //
        }
    }
    
    static String getProperty(String name)
    {
        return config.getProperty(name);
    }
    
    static int getIntProperty(String name)
    {
        return config.getIntProperty(name);
    }
    
    static boolean getBoolProperty(String name)
    {
        return config.getBoolProperty(name);
    }
    
    static EntityPlayer getEntityPlayerById(int id)
    {
        Object[] players = ModLoader.getMinecraftInstance().theWorld.playerEntities.toArray();

        for (int i = 0; i < players.length; i++)
        {
            EntityPlayer ep = (EntityPlayer) players[i];
            if (ep != null && ep.entityId == id) return ep;
        }
        return null;
    }
    
    /** 
     * This method is called to spawn new entities on the client based on Packet23VehicleSpawn
     * packets from the server. 
     * @param World world - the WorldClient instance for the current client
     */
    @Override
    public Entity spawnEntity(int type, World world, double posX, double posY, double posZ)
    {
        if (type == HELICOPTER_TYPE_ID)
        {
            return new ThxEntityHelicopter(world, posX, posY, posZ, 0f);
        }
        
        if (type == ROCKET_TYPE_ID)
        {
            return new ThxEntityRocket(world, posX, posY, posZ);
        }
        
        if (type == MISSILE_TYPE_ID)
        {
            return new ThxEntityMissile(world, posX, posY, posZ);
        }
        
        return null;
    }
    
    @Override
    public Packet23VehicleSpawn getSpawnPacket(Entity entity, int type)
    {
        log("Creating spawn packet for entity: " + entity);
        return new Packet23VehicleSpawn(entity, type, 1); // 1 is the id of the "thrower", it will cause velocity to get updated at spawn
    }
    
    @Override
    public void clientCustomPayload(NetClientHandler netHandler, Packet250CustomPayload packet)
    {
        EntityClientPlayerMP thePlayer = ModLoader.getMinecraftInstance().thePlayer;
        
	    if ("THX_entity".equals(packet.channel))
	    {
	        ThxEntityPacket250 data = new ThxEntityPacket250(packet);
	        plog("Client received: " + data);
	        
	        ThxEntity entity = (ThxEntity) thePlayer.worldObj.getEntityByID(data.entityId);
	        if (entity != null)
            {
	            entity.lastUpdatePacket = data;
            }
	        else
	        {
	            log("client received client update packet for unknown entity id " + data.entityId); // probably never happen
	        }
	    }
    }

    @Override
    public void serverCustomPayload(NetServerHandler netHandler, Packet250CustomPayload packet)
    {
        EntityPlayerMP thePlayer = netHandler.playerEntity;
        
	    if ("THX_entity".equals(packet.channel))
	    {
	        ThxEntityPacket250 data = new ThxEntityPacket250(packet);
	        plog("Server received: " + data);
	        
	        ThxEntity entity = (ThxEntity) thePlayer.worldObj.getEntityByID(data.entityId);
	        if (entity != null)
            {
	            entity.lastUpdatePacket = data;
            }
	        else
	        {
	            log("received client update packet for unknown entity id " + data.entityId);
	        }
	    }
    }
}
