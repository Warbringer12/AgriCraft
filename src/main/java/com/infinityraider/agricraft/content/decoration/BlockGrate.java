package com.infinityraider.agricraft.content.decoration;

import com.infinityraider.agricraft.AgriCraft;
import com.infinityraider.agricraft.reference.Names;
import com.infinityraider.infinitylib.block.BlockDynamicTexture;
import com.infinityraider.infinitylib.block.property.InfProperty;
import com.infinityraider.infinitylib.block.property.InfPropertyConfiguration;
import com.infinityraider.infinitylib.reference.Constants;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockGrate extends BlockDynamicTexture<TileEntityGrate> {
    // Properties
    public static final InfProperty<Direction.Axis> AXIS = InfProperty.Creators.create("direction", Direction.Axis.X);
    public static final InfProperty<Offset> OFFSET = InfProperty.Creators.create("offset", Offset.class, Offset.MID);
    public static final InfProperty<Vines> VINES = InfProperty.Creators.create("vines", Vines.class, Vines.NONE);

    private static final InfPropertyConfiguration PROPERTIES = InfPropertyConfiguration.builder()
            .add(AXIS)
            .add(OFFSET)
            .add(VINES)
            .waterloggable()
            .build();

    // TileEntity factory
    private static final BiFunction<BlockState, IBlockReader, TileEntityGrate> TILE_FACTORY = (s, w) -> new TileEntityGrate();

    public BlockGrate() {
        super(Names.Blocks.GRATE, Properties.create(Material.WOOD)
                .notSolid()
        );
    }

    // VoxelShapes
    protected static VoxelShape getShape(Direction.Axis axis, Offset offset) {
        switch (axis) {
            case X: return getShape(X_SHAPES, offset);
            case Y: return getShape(Y_SHAPES, offset);
            case Z: return getShape(Z_SHAPES, offset);
            // Should never happen
            default: return VoxelShapes.empty();
        }
    }

    public static final VoxelShape X_DEFAULT = Stream.of(
            Block.makeCuboidShape(7, 1, 0, 9, 3, 16),
            Block.makeCuboidShape(7, 5, 0, 9, 7, 16),
            Block.makeCuboidShape(7, 9, 0, 9, 11, 16),
            Block.makeCuboidShape(7, 13, 0, 9, 15, 16),
            Block.makeCuboidShape(7, 0, 1, 9, 16, 3),
            Block.makeCuboidShape(7, 0, 5, 9, 16, 7),
            Block.makeCuboidShape(7, 0, 9, 9, 16, 11),
            Block.makeCuboidShape(7, 0, 13, 9, 16, 15)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, IBooleanFunction.OR)).get();

    public static final VoxelShape X_NEAR = X_DEFAULT.withOffset(Offset.NEAR.getOffset(), 0, 0);

    public static final VoxelShape X_FAR = X_DEFAULT.withOffset(Offset.FAR.getOffset(), 0, 0);

    public static final VoxelShape[] X_SHAPES = {X_NEAR, X_DEFAULT, X_FAR};

    public static final VoxelShape Y_DEFAULT = Stream.of(
            Block.makeCuboidShape(1, 7, 0, 3, 9, 16),
            Block.makeCuboidShape(5, 7, 0, 7, 9, 16),
            Block.makeCuboidShape(9, 7, 0, 11, 9, 16),
            Block.makeCuboidShape(13, 7, 0, 15, 9, 16),
            Block.makeCuboidShape(0, 7, 1, 16, 9, 3),
            Block.makeCuboidShape(0, 7, 5, 16, 9, 7),
            Block.makeCuboidShape(0, 7, 9, 16, 9, 11),
            Block.makeCuboidShape(0, 7, 13, 16, 9, 15)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, IBooleanFunction.OR)).get();

    public static final VoxelShape Y_NEAR = Y_DEFAULT.withOffset(0, Offset.NEAR.getOffset(), 0);

    public static final VoxelShape Y_FAR = Y_DEFAULT.withOffset(0, Offset.FAR.getOffset(), 0);

    public static final VoxelShape[] Y_SHAPES = {Y_NEAR, Y_DEFAULT, Y_FAR};

    public static final VoxelShape Z_DEFAULT = Stream.of(
            Block.makeCuboidShape(1, 0, 7, 3, 16, 9),
            Block.makeCuboidShape(5, 0, 7, 7, 16, 9),
            Block.makeCuboidShape(9, 0, 7, 11, 16, 9),
            Block.makeCuboidShape(13, 0, 7, 15, 16, 9),
            Block.makeCuboidShape(0, 1, 7, 16, 3, 9),
            Block.makeCuboidShape(0, 5, 7, 16, 7, 9),
            Block.makeCuboidShape(0, 9, 7, 16, 11, 9),
            Block.makeCuboidShape(0, 13, 7, 16, 15, 9)
    ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, IBooleanFunction.OR)).get();

    public static final VoxelShape Z_NEAR = Z_DEFAULT.withOffset(0, 0, Offset.NEAR.getOffset());

    public static final VoxelShape Z_FAR = Z_DEFAULT.withOffset(0, 0, Offset.FAR.getOffset());

    public static final VoxelShape[] Z_SHAPES = {Z_NEAR, Z_DEFAULT, Z_FAR};

    @Override
    protected InfPropertyConfiguration getPropertyConfiguration() {
        return PROPERTIES;
    }

    @Override
    public BiFunction<BlockState, IBlockReader, TileEntityGrate> getTileEntityFactory() {
        return TILE_FACTORY;
    }

    @Override
    public Item asItem() {
        return AgriCraft.instance.getModItemRegistry().grate;
    }

    @Override
    public void onBlockPlacedBy(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull BlockState state,
                                @Nullable LivingEntity placer, @Nonnull ItemStack stack, @Nullable TileEntity tile) {
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public boolean isValidPosition(BlockState state, IWorldReader world, BlockPos pos) {
        BlockState current = world.getBlockState(pos);
        return current.getMaterial().isReplaceable();
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        BlockState state = this.getDefaultState();
        if(state.isValidPosition(context.getWorld(), context.getPos())) {
            BlockPos clicked = context.getPos().offset(context.getFace().getOpposite());
            BlockState target = context.getWorld().getBlockState(clicked);
            if(target.getBlock() instanceof BlockGrate) {
                // If a grate is clicked on a grate, mimic its placement
                return AXIS.apply(
                        OFFSET.apply(
                                this.waterlog(state, context.getWorld(), context.getPos()),
                                OFFSET.fetch(target)
                        ),
                        AXIS.fetch(target)
                );
            } else {
                // If not, determine the axis according to the face the player clicked and his look vector
                Vector3d hit = context.getHitVec();
                double offset;
                if(context.getFace().getAxis() == Direction.Axis.Y) {
                    // The player clicked a horizontal face, determine the axis based on the player's orientation
                    if(context.getPlacementHorizontalFacing().getAxis() == Direction.Axis.X) {
                        // player is looking in the X direction
                        state = AXIS.apply(state, Direction.Axis.X);
                        offset = hit.getX() - ((int) hit.getX());
                    } else {
                        // player is looking in the Z direction
                        state = AXIS.apply(state, Direction.Axis.Z);
                        offset = hit.getZ() - ((int) hit.getZ());
                    }
                } else {
                    // The player clicked a vertical face, the axis will be Y
                    state = AXIS.apply(state, Direction.Axis.Y);
                    offset = hit.getY() - ((int) hit.getY());
                }
                // Finally, determine the offset by how far along the block the player clicked
                offset += offset < 0 ? 1 : 0;
                if(offset >= 11*Constants.UNIT) {
                    return OFFSET.apply(
                            this.waterlog(state, context.getWorld(), context.getPos()),
                            Offset.FAR);
                } else if(offset <= 5*Constants.UNIT) {
                    return OFFSET.apply(
                            this.waterlog(state, context.getWorld(), context.getPos()),
                            Offset.NEAR);
                } else {
                    return OFFSET.apply(
                            this.waterlog(state, context.getWorld(), context.getPos()),
                            Offset.MID);
                }
            }
        }
        return null;
    }

    @Override
    public boolean isLadder(BlockState state, IWorldReader world, BlockPos pos, LivingEntity entity) {
        return AgriCraft.instance.getConfig().areGratesClimbable() && (AXIS.fetch(state) != Direction.Axis.Y);
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public VoxelShape getRenderShape(BlockState state, IBlockReader world, BlockPos pos) {
        return this.getShape(state, world, pos, ISelectionContext.dummy());
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos) {
        return this.getCollisionShape(state, world, pos, ISelectionContext.dummy());
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public VoxelShape getRaytraceShape(BlockState state, IBlockReader world, BlockPos pos) {
        return this.getRayTraceShape(state, world, pos, ISelectionContext.dummy());
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        return getShape(AXIS.fetch(state), OFFSET.fetch(state));
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        return this.getShape(state, world, pos, context);
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public VoxelShape getRayTraceShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        return this.getShape(state, world, pos, context);
    }

    protected static VoxelShape getShape(VoxelShape[] shapes, Offset offset) {
        return shapes[offset.ordinal()];
    }

    public enum Offset implements IStringSerializable {
        NEAR(-7*Constants.UNIT),
        MID(0),
        FAR(7*Constants.UNIT);

        private final double offset;

        Offset(double offset) {
            this.offset = offset;
        }

        @Override
        public String getString() {
            return this.name().toLowerCase();
        }

        public double getOffset() {
            return this.offset;
        }
    }

    public enum Vines implements IStringSerializable {
        NONE,
        FRONT,
        BACK,
        BOTH;

        @Override
        public String getString() {
            return this.name().toLowerCase();
        }
    }
}
