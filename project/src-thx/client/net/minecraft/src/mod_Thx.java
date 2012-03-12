package net.minecraft.src;

public class mod_Thx extends BaseModMp
{
    public static mod_Thx instance;

    public mod_Thx()
    {
        log("mod_Thx() called");
        instance = this; // for easy access by static methods and to instance methods
    }

    @Override
    public void load()
    {
        log("load() called");

        ThxConfig.loadProperties();

        ModLoader.setInGameHook(this, true, true);

        // register entity classes
        helicopter:
        {
            ModLoaderMp.registerNetClientHandlerEntity(ThxEntityHelicopter.class, 75);
            
            int entityId = ModLoader.getUniqueEntityId();
            log("Registering entity class for Helicopter with entity id " + entityId);
            ModLoader.registerEntityID(ThxEntityHelicopter.class, "thxHelicopter", entityId);
        }
        rocket:
        {
            ModLoaderMp.registerNetClientHandlerEntity(ThxEntityRocket.class, 76);
            
            int entityId = ModLoader.getUniqueEntityId();
            log("Registering entity class for Rocket with entity id " + entityId);
            ModLoader.registerEntityID(ThxEntityRocket.class, "thxRocket", entityId);
        }
        missile:
        {
            ModLoaderMp.registerNetClientHandlerEntity(ThxEntityMissile.class, 77);
            
            int entityId = ModLoader.getUniqueEntityId();
            log("Registering entity class for Missile with entity id " + entityId);
            ModLoader.registerEntityID(ThxEntityMissile.class, "thxMissile", entityId);
        }

        helicopterItem:
        {
            int itemId = getNextItemId();
            log("Setting up inventory item for helicopter with item id " + itemId);
            Item item = new ThxItemHelicopter(itemId);

            if (ThxConfig.getBoolProperty("disable_helicopter_item_image"))
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
        return "Minecraft THX Helicopter Mod - mod_thx-mc123_v016_bp1";
    }

    @Override
    public void handlePacket(Packet230ModLoader packet)
    {
        if (ThxConfig.LOG_INCOMING_PACKETS) log("received incoming server packet: " + packetToString(packet));
        
        int entityId = packet.dataInt[0];
        if (entityId < 1) log("Received non-entity packet type " + packet.packetType + ": " + packet);
        else
        {
	        Entity entity = ((WorldClient) ModLoader.getMinecraftInstance().theWorld).getEntityByID(entityId);
	        if (entity instanceof ThxEntity) ((ThxEntity) entity).latestUpdatePacket = packet;
        }
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

    public static void log(String s)
    {
        if (ThxConfig.ENABLE_LOGGING) System.out.println("mod_thx_client: " + s);
    }

    public String packetToString(Packet230ModLoader p)
    {
        StringBuffer s = new StringBuffer();
        s.append("Packet230 {");
        s.append("type: ").append(p.packetType).append(", ");
        s.append("modId: ").append(p.modId).append(", ");

        for (int i = 0; p.dataInt != null && i < p.dataInt.length; i++)
        {
            s.append("dataInt[" + i + "]: ");
            s.append(p.dataInt[i]);
            s.append(", ");
        }
        for (int i = 0; p.dataFloat != null && i < p.dataFloat.length; i++)
        {
            s.append("dataFloat[" + i + "]: ");
            s.append(p.dataFloat[i]);
            s.append(", ");
        }
        for (int i = 0; p.dataDouble != null && i < p.dataDouble.length; i++) 
        {
            s.append("dataDouble[" + i + "]: "); 
            s.append(p.dataDouble[i]); 
            s.append(", "); 
        }
        for (int i = 0; p.dataString != null && i < p.dataString.length; i++)
        {
            s.append("dataString[" + i + "]: ");
            s.append(p.dataString[i]);
            s.append(", ");
        }
        s.append("}");

        return s.toString();
    }
}
