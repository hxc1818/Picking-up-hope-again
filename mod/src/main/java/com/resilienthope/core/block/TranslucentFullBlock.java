package com.resilienthope.core.block;

import net.minecraft.world.level.block.AbstractGlassBlock;

/**
 * Exposes AbstractGlassBlock's protected constructor for translucent glacier ice and hardlight blocks
 * with clean adjacent-face culling.
 */
public class TranslucentFullBlock extends AbstractGlassBlock {
    public TranslucentFullBlock(Properties properties) {
        super(properties);
    }
}
