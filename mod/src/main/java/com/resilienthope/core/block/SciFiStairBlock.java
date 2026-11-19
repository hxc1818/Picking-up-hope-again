package com.resilienthope.core.block;

import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Convenience wrapper for StairBlock across starship, cryo planet, and alien ruin materials.
 */
public class SciFiStairBlock extends StairBlock {
    public SciFiStairBlock(Supplier<BlockState> baseState, Properties properties) {
        super(baseState, properties);
    }
}
