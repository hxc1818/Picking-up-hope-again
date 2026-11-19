package com.resilienthope.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Horizontal 4-way rotatable 3D small voxel prop block with accurate shape rotation for N/E/S/W.
 */
public class RotatablePropBlock extends HorizontalDirectionalBlock {
    private final VoxelShape northShape;
    private final VoxelShape eastShape;
    private final VoxelShape southShape;
    private final VoxelShape westShape;

    public RotatablePropBlock(int lightLevel, VoxelShape northShape) {
        this(Material.METAL, SoundType.METAL, lightLevel, northShape, true);
    }

    public RotatablePropBlock(Material material, SoundType sound, int lightLevel, VoxelShape northShape, boolean hasCollision) {
        super((hasCollision ? BlockBehaviour.Properties.of(material) : BlockBehaviour.Properties.of(material).noCollission())
                .strength(2.5f, 8.0f)
                .sound(sound)
                .lightLevel(s -> lightLevel)
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.northShape = northShape;
        this.eastShape = rotateClockwise(northShape);
        this.southShape = rotateClockwise(this.eastShape);
        this.westShape = rotateClockwise(this.southShape);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> eastShape;
            case SOUTH -> southShape;
            case WEST -> westShape;
            default -> northShape;
        };
    }

    private static VoxelShape rotateClockwise(VoxelShape source) {
        final VoxelShape[] result = {Shapes.empty()};
        source.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            VoxelShape box = Shapes.box(1.0D - maxZ, minY, minX, 1.0D - minZ, maxY, maxX);
            result[0] = Shapes.or(result[0], box);
        });
        return result[0];
    }
}
