package com.resilienthope.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Decorative 3D voxel prop block with customizable hitbox and light level.
 */
public class VoxelPropBlock extends Block {
    private final VoxelShape shape;

    public VoxelPropBlock(int lightLevel, VoxelShape shape) {
        super(BlockBehaviour.Properties.of(Material.METAL)
                .strength(4.0f, 12.0f)
                .sound(SoundType.METAL)
                .lightLevel(state -> lightLevel)
                .noOcclusion());
        this.shape = shape;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }
}
