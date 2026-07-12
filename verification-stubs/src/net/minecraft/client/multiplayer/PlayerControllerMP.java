package net.minecraft.client.multiplayer;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

public class PlayerControllerMP {
    private int rightClicks;

    public void updateController() { }

    public boolean onPlayerRightClick(EntityPlayerSP player, WorldClient world, ItemStack stack,
                                      BlockPos pos, EnumFacing face, Vec3 hitVec) {
        rightClicks++;
        if (stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemBlock)) {
            return false;
        }
        BlockPos target = new BlockPos(
            pos.getX() + face.getFrontOffsetX(),
            pos.getY() + face.getFrontOffsetY(),
            pos.getZ() + face.getFrontOffsetZ());
        world.setBlockStateForTest(target,
            ((ItemBlock)stack.getItem()).getBlock().getDefaultState());
        stack.stackSize--;
        return true;
    }

    public int rightClicksForTest() {
        return rightClicks;
    }
}
