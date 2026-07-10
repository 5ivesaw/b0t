package net.minecraft.util;
public class AxisAlignedBB {
    public final double minX,minY,minZ,maxX,maxY,maxZ;
    public AxisAlignedBB(double minX,double minY,double minZ,double maxX,double maxY,double maxZ){this.minX=minX;this.minY=minY;this.minZ=minZ;this.maxX=maxX;this.maxY=maxY;this.maxZ=maxZ;}
    public boolean intersectsWith(AxisAlignedBB other){return other.maxX>minX&&other.minX<maxX&&other.maxY>minY&&other.minY<maxY&&other.maxZ>minZ&&other.minZ<maxZ;}
}
