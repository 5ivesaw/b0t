package net.minecraft.entity;
import java.util.IdentityHashMap;
import java.util.Map;
public final class EntityList {
    private static final Map<Entity,String> NAMES=new IdentityHashMap<Entity,String>();
    private EntityList(){}
    public static String getEntityString(Entity entity){return NAMES.get(entity);}
    public static void setEntityStringForTest(Entity entity,String name){if(name==null)NAMES.remove(entity);else NAMES.put(entity,name);}
}
