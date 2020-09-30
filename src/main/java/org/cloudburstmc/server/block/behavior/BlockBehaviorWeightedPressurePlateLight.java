package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorWeightedPressurePlateLight extends BlockBehaviorPressurePlateBase {

    public BlockBehaviorWeightedPressurePlateLight() {
        this.onPitch = 0.90000004f;
        this.offPitch = 0.75f;
    }


    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public ToolType getToolType(BlockState state) {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemToolBehavior.TIER_WOODEN) {
            return new ItemStack[]{
                    toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.GOLD_BLOCK_COLOR;
    }

    @Override
    protected int computeRedstoneStrength(Block block) {
        int count = Math.min(block.getLevel().getCollidingEntities(getCollisionBoxes(block)).size(), this.getMaxWeight());

        if (count > 0) {
            float f = (float) Math.min(this.getMaxWeight(), count) / (float) this.getMaxWeight();
            return NukkitMath.ceilFloat(f * 15.0F);
        } else {
            return 0;
        }
    }

    public int getMaxWeight() {
        return 15;
    }
}