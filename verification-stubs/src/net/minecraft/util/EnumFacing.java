package net.minecraft.util;
public enum EnumFacing {
    DOWN(0,-1,0), UP(0,1,0), NORTH(0,0,-1), SOUTH(0,0,1), WEST(-1,0,0), EAST(1,0,0);
    private final int x,y,z;
    EnumFacing(int x,int y,int z){this.x=x;this.y=y;this.z=z;}
    public int getFrontOffsetX(){return x;}
    public int getFrontOffsetY(){return y;}
    public int getFrontOffsetZ(){return z;}
    public EnumFacing getOpposite(){switch(this){case DOWN:return UP;case UP:return DOWN;case NORTH:return SOUTH;case SOUTH:return NORTH;case WEST:return EAST;default:return WEST;}}
}
