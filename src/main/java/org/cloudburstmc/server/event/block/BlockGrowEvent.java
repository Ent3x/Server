package org.cloudburstmc.server.event.block;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockGrowEvent extends BlockEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final BlockState newState;

    public BlockGrowEvent(Block block, BlockState newState) {
        super(block);
        this.newState = newState;
    }

    public BlockState getNewState() {
        return newState;
    }

}
