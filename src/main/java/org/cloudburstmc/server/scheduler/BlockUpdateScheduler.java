package org.cloudburstmc.server.scheduler;

import com.google.common.collect.Maps;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.math.vector.Vector4i;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.utils.BlockUpdateEntry;

import java.util.*;

@Log4j2
public class BlockUpdateScheduler {
    private final Level level;
    private long lastTick;
    private Map<Long, LinkedHashSet<BlockUpdateEntry>> queuedUpdates;

    private Set<BlockUpdateEntry> pendingUpdates;

    public BlockUpdateScheduler(Level level, long currentTick) {
        queuedUpdates = Maps.newHashMap(); // Change to ConcurrentHashMap if this needs to be concurrent
        lastTick = currentTick;
        this.level = level;
    }

    public synchronized void tick(long currentTick) {
        // Should only perform once, unless ticks were skipped
        if (currentTick - lastTick < Short.MAX_VALUE) {// Arbitrary
            for (long tick = lastTick + 1; tick <= currentTick; tick++) {
                perform(tick);
            }
        } else {
            ArrayList<Long> times = new ArrayList<>(queuedUpdates.keySet());
            Collections.sort(times);
            for (long tick : times) {
                if (tick <= currentTick) {
                    perform(tick);
                } else {
                    break;
                }
            }
        }
        lastTick = currentTick;
    }

    private void perform(long tick) {
        try {
            lastTick = tick;
            Set<BlockUpdateEntry> updates = pendingUpdates = queuedUpdates.remove(tick);
            if (updates != null) {
                for (BlockUpdateEntry entry : updates) {
                    Vector3i pos = entry.pos;
                    if (level.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
                        Block block = level.getBlock(entry.pos);
                        val state = block.getState();
                        val extra = block.getExtra();

                        if (entry.block.getState() == state) {
                            state.getBehavior().onUpdate(block, Level.BLOCK_UPDATE_SCHEDULED);
                        }

                        if (entry.block.getExtra() == extra && extra != BlockStates.AIR) {
                            extra.getBehavior().onUpdate(block, Level.BLOCK_UPDATE_SCHEDULED);
                        }
                    } else {
                        level.scheduleUpdate(entry.block, entry.pos, 0);
                    }
                }
            }
        } finally {
            pendingUpdates = null;
        }
    }

    public Set<BlockUpdateEntry> getPendingBlockUpdates(AxisAlignedBB boundingBox) {
        Set<BlockUpdateEntry> set = null;

        for (Map.Entry<Long, LinkedHashSet<BlockUpdateEntry>> tickEntries : this.queuedUpdates.entrySet()) {
            LinkedHashSet<BlockUpdateEntry> tickSet = tickEntries.getValue();
            for (BlockUpdateEntry update : tickSet) {
                Vector3i pos = update.pos;

                if (pos.getX() >= boundingBox.getMinX() && pos.getX() < boundingBox.getMaxX() && pos.getZ() >= boundingBox.getMinZ() && pos.getZ() < boundingBox.getMaxZ()) {
                    if (set == null) {
                        set = new LinkedHashSet<>();
                    }

                    set.add(update);
                }
            }
        }

        return set;
    }

    public boolean isBlockTickPending(Vector3i pos, Block blockState) {
        Set<BlockUpdateEntry> tmpUpdates = pendingUpdates;
        if (tmpUpdates == null || tmpUpdates.isEmpty()) return false;
        return tmpUpdates.contains(new BlockUpdateEntry(pos, blockState));
    }

    private long getMinTime(BlockUpdateEntry entry) {
        return Math.max(entry.delay, lastTick + 1);
    }

    public void add(BlockUpdateEntry entry) {
        long time = getMinTime(entry);
        LinkedHashSet<BlockUpdateEntry> updateSet = queuedUpdates.get(time);
        if (updateSet == null) {
            LinkedHashSet<BlockUpdateEntry> tmp = queuedUpdates.putIfAbsent(time, updateSet = new LinkedHashSet<>());
            if (tmp != null) updateSet = tmp;
        }
        updateSet.add(entry);
    }

    public boolean contains(BlockUpdateEntry entry) {
        for (Map.Entry<Long, LinkedHashSet<BlockUpdateEntry>> tickUpdateSet : queuedUpdates.entrySet()) {
            if (tickUpdateSet.getValue().contains(entry)) {
                return true;
            }
        }
        return false;
    }

    public boolean remove(BlockUpdateEntry entry) {
        for (Map.Entry<Long, LinkedHashSet<BlockUpdateEntry>> tickUpdateSet : queuedUpdates.entrySet()) {
            if (tickUpdateSet.getValue().remove(entry)) {
                return true;
            }
        }
        return false;
    }

    public boolean remove(Vector4i pos) {
        for (Map.Entry<Long, LinkedHashSet<BlockUpdateEntry>> tickUpdateSet : queuedUpdates.entrySet()) {
            if (tickUpdateSet.getValue().remove(pos)) {
                return true;
            }
        }
        return false;
    }
}
