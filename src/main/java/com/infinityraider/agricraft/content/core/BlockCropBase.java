package com.infinityraider.agricraft.content.core;

import com.infinityraider.agricraft.api.v1.AgriApi;
import com.infinityraider.agricraft.api.v1.crop.IAgriCrop;
import com.infinityraider.infinitylib.block.BlockBaseTile;
import com.infinityraider.infinitylib.block.property.InfProperty;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.IPlantable;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.Random;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BlockCropBase<T extends TileEntityCropBase> extends BlockBaseTile<T> implements IWaterLoggable, IGrowable, IPlantable {
    // Plant property
    public static final InfProperty<Boolean> PLANT = InfProperty.Creators.create("plant", false);

    public BlockCropBase(String name, Properties properties) {
        super(name, properties);
    }

    public Optional<IAgriCrop> getCrop(IBlockReader world, BlockPos pos) {
        return AgriApi.getCrop(world, pos);
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
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if(fromPos.up().equals(pos)) {
            if(!state.isValidPosition(world, pos)) {
                this.breakBlock(state, world, pos, true);
            }
        }
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public boolean isValidPosition(BlockState state, IWorldReader world, BlockPos pos) {
        BlockState current = world.getBlockState(pos);
        return current.getMaterial().isReplaceable() && AgriApi.getSoilRegistry().valueOf(world.getBlockState(pos.down())).isPresent();
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getStateForPlacement(context.getWorld(), context.getPos());
    }

    @Nullable
    public BlockState getStateForPlacement(World world, BlockPos pos) {
        BlockState state = this.getDefaultState();
        if(state.isValidPosition(world, pos)) {
            return this.waterlog(state, world, pos);
        }
        return null;
    }

    public void breakBlock(BlockState state, World world, BlockPos pos, boolean doDrops) {
        if(!world.isRemote()) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            if(doDrops) {
                spawnDrops(state, world, pos, world.getTileEntity(pos));
            }
        }
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        this.getCrop(world, pos).ifPresent(IAgriCrop::applyGrowthTick);
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public void onBlockClicked(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        this.getCrop(world, pos).ifPresent(crop -> crop.breakCrop(player));
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        this.getCrop(world, pos).ifPresent(crop -> crop.getPlant().onEntityCollision(crop, entity));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public RenderType getRenderType() {
        return RenderType.getCutout();
    }

    public void spawnItem(IAgriCrop crop, ItemStack stack) {
        World world = crop.getWorld();
        if(world != null) {
            this.spawnItem(world, crop.getPosition(), stack);
        }
    }

    /**
     * -------------------------
     * Vanilla IGrowable methods
     * -------------------------
     */

    @Override
    public boolean canGrow(IBlockReader world, BlockPos pos, BlockState state, boolean isClient) {
        return this.isFertile(state, world, pos);
    }

    private static final ItemStack BONE_MEAL = new ItemStack(Items.BONE_MEAL);

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, BlockState state) {
        return AgriApi.getFertilizerAdapterizer().valueOf(BONE_MEAL)
                .flatMap(fertilizer ->
                        this.getCrop(world, pos).map(crop ->
                                crop.acceptsFertilizer(fertilizer)))
                .orElse(false);
    }

    @Override
    public void grow(ServerWorld world, Random rand, BlockPos pos, BlockState state) {
        AgriApi.getFertilizerAdapterizer().valueOf(BONE_MEAL).ifPresent(fertilizer ->
                this.getCrop(world, pos).ifPresent(crop ->
                        fertilizer.applyFertilizer(world, pos, crop, BONE_MEAL, rand, null)));
    }

    /**
     * ------------------------
     * Forge IPlantable methods
     * ------------------------
     */

    @Override
    public BlockState getPlant(IBlockReader world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if(world instanceof World) {
            return this.getCrop(world, pos)
                    .flatMap(crop -> crop.getPlant().asBlockState(crop.getGrowthStage()))
                    .orElse(state);
        }
        return state;
    }
}
