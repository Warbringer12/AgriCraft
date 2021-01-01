package com.infinityraider.agricraft.blocks.core;

import com.agricraft.agricore.util.TypeHelper;
import com.google.common.collect.Lists;
import com.infinityraider.agricraft.AgriCraft;
import com.infinityraider.agricraft.api.v1.AgriApi;
import com.infinityraider.agricraft.api.v1.crop.IAgriCrop;
import com.infinityraider.agricraft.api.v1.items.IAgriClipperItem;
import com.infinityraider.agricraft.api.v1.items.IAgriRakeItem;
import com.infinityraider.agricraft.api.v1.items.IAgriTrowelItem;
import com.infinityraider.agricraft.api.v1.plant.IAgriGrowthStage;
import com.infinityraider.agricraft.api.v1.plant.IAgriPlant;
import com.infinityraider.agricraft.api.v1.plant.IAgriWeed;
import com.infinityraider.agricraft.items.core.ItemDebugger;
import com.infinityraider.agricraft.reference.Names;
import com.infinityraider.infinitylib.block.BlockBase;
import com.infinityraider.infinitylib.block.property.InfProperty;
import com.infinityraider.infinitylib.block.property.InfPropertyConfiguration;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.IPlantable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

public class BlockCropSticks extends BlockBase implements IGrowable, IPlantable {
    private static final Class[] ITEM_EXCLUDES = new Class[]{
            IAgriRakeItem.class,
            IAgriClipperItem.class,
            IAgriTrowelItem.class,
            ItemDebugger.class
    };

    public static final InfProperty<Boolean> CROSS_CROP = InfProperty.Creators.create("cross_crop", false);
    public static final InfProperty<IAgriPlant> PLANT = InfProperty.Creators.create(PropertyAgriRegisterable.getPlantProperty(), null); //TODO: no plant
    public static final InfProperty<IAgriGrowthStage> GROWTH = InfProperty.Creators.create(PropertyAgriRegisterable.getGrowthProperty(), null); //TODO: growth stages
    public static final InfProperty<IAgriWeed> WEED = InfProperty.Creators.create(PropertyAgriRegisterable.getWeedProperty(), null); //TODO: no weed
    public static final InfProperty<IAgriGrowthStage> WEED_GROWTH = InfProperty.Creators.create(PropertyAgriRegisterable.getWeedGrowthProperty(), null); //TODO: growth stages

    private static final InfPropertyConfiguration PROPERTIES = InfPropertyConfiguration.builder()
            .add(CROSS_CROP)
            .add(PLANT)
            .add(GROWTH)
            .add(WEED)
            .add(WEED_GROWTH)
            .waterloggable()
            .build();

    private final VoxelShape shapeDefault;
    private final VoxelShape shapeCrossCrop;

    public BlockCropSticks(String variant, Material material) {
        super(Names.Blocks.CROP_STICKS + "_" + variant, Properties.create(material).tickRandomly());
        this.shapeDefault = Stream.of(
                Block.makeCuboidShape(2, -3, 2, 3, 13, 3),
                Block.makeCuboidShape(13, -3, 2, 14, 13, 3),
                Block.makeCuboidShape(2, -3, 13, 3, 13, 14),
                Block.makeCuboidShape(13, -3, 13, 14, 13, 14)
        ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, IBooleanFunction.OR)).get();
        this.shapeCrossCrop = Stream.of(
                this.shapeDefault,
                Block.makeCuboidShape(0, 7, 2, 16, 9, 3),
                Block.makeCuboidShape(0, 7, 13, 16, 9, 14),
                Block.makeCuboidShape(2, 7, 0, 3, 9, 16),
                Block.makeCuboidShape(13, 7, 0, 14, 9, 16)
        ).reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, IBooleanFunction.OR)).get();
    }

    @Override
    protected InfPropertyConfiguration getPropertyConfiguration() {
        return PROPERTIES;
    }

    @Override
    public Item asItem() {
        return AgriCraft.instance.getModItemRegistry().crop_sticks;
    }

    @Override
    @Deprecated
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if(fromPos.up().equals(pos)) {
            if(!state.isValidPosition(world, pos)) {
                this.breakBlock(state, world, pos, true);
            }
        }
    }

    @Override
    @Deprecated
    public boolean isValidPosition(BlockState state, IWorldReader world, BlockPos pos) {
        BlockState current = world.getBlockState(pos);
        return current.getMaterial().isReplaceable() && AgriApi.getSoilRegistry().contains(world.getBlockState(pos.down()));
    }


    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        BlockState state = this.getDefaultState();
        if(state.isValidPosition(context.getWorld(), context.getPos())) {
            return this.waterlog(state, context.getWorld(), context.getPos());
        }
        return null;
    }

    public void breakBlock(BlockState state, World world, BlockPos pos, boolean doDrops) {
        if(!world.isRemote()) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            if(doDrops) {
                spawnDrops(state, world, pos);
            }
        }
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        AgriApi.getCrop(state, world, pos).ifPresent(IAgriCrop::applyGrowthTick);
    }

    @Override
    @Deprecated
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if(world.isRemote()) {
            return ActionResultType.PASS;
        }
        Optional<IAgriCrop> optional = AgriApi.getCrop(state, world, pos);
        if(!optional.isPresent()) {
            return ActionResultType.PASS;
        }
        IAgriCrop crop = optional.get();
        ItemStack heldItem = player.getHeldItem(hand);
        // Harvesting
        if(heldItem.isEmpty()) {
            return crop.harvest(stack -> this.spawnItem(crop, stack), player);
        }
        // Specific item interactions
        if(TypeHelper.isAnyType(heldItem.getItem(), ITEM_EXCLUDES)) {
            return ActionResultType.PASS;
        }
        // Fertilization
        if(AgriApi.getFertilizerRegistry().hasAdapter(heldItem)) {
            return AgriApi.getFertilizerRegistry().valueOf(heldItem)
                    .map(f -> f.applyFertilizer(crop, heldItem, world.getRandom()))
                    .orElse(ActionResultType.PASS);
        }
        // Creation of Cross crops
        if(heldItem.getItem() == this.asItem()) {
            if(crop.setCrossCrop(true)) {
                if(!player.isCreative()) {
                    player.getHeldItem(hand).shrink(1);
                }
                return ActionResultType.CONSUME;
            }
        }
        // Planting
        if(AgriApi.getSeedRegistry().hasAdapter(heldItem)) {
           return AgriApi.getSeedRegistry().valueOf(heldItem)
                    .map(seed -> {
                        if(crop.setSeed(seed)) {
                            if(!player.isCreative()) {
                                player.getHeldItem(hand).shrink(1);
                            }
                            return ActionResultType.CONSUME;
                        } else {
                            return ActionResultType.PASS;
                        }
                    })
                    .orElse(ActionResultType.PASS);
        }
        // Fall Back to harvesting
        return crop.harvest(stack -> this.spawnItem(crop, stack), player);
    }

    @Override
    @Deprecated
    public void onBlockClicked(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        AgriApi.getCrop(state, world, pos).ifPresent(IAgriCrop::breakCrop);
    }

    @Override
    @Deprecated
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder context) {
        List<ItemStack> drops = Lists.newArrayList();
        drops.add(new ItemStack(this.asItem(), 1));
        if(CROSS_CROP.fetch(state)) {
            drops.add(new ItemStack(this.asItem(), 1));
        } else {
            PLANT.fetch(state).getHarvestProducts(drops::add, GROWTH.fetch(state), null, context.getWorld().getRandom());
        }
        return drops;
    }
    @Override
    @OnlyIn(Dist.CLIENT)
    public RenderType getRenderType() {
        return RenderType.getCutout();
    }

    public void spawnItem(IAgriCrop crop, ItemStack stack) {
        this.spawnItem(crop.getWorld(), crop.getPosition(), stack);
    }

    /**
     * ---------------------------------------
     * BlockState property getters and setters
     * ---------------------------------------
     */
    public boolean isCrossCrop(BlockState state) {
        return CROSS_CROP.fetch(state);
    }

    public IAgriPlant getPlant(BlockState state) {
        return PLANT.fetch(state);
    }

    public IAgriGrowthStage getGrowthStage(BlockState state) {
        return GROWTH.fetch(state);
    }

    public IAgriWeed getWeed(BlockState state) {
        return WEED.fetch(state);
    }

    public IAgriGrowthStage getWeedGrowthStage(BlockState state) {
        return WEED_GROWTH.fetch(state);
    }

    public BlockState setCrossCrop(BlockState state, boolean value) {
        return CROSS_CROP.apply(state, value);
    }

    public BlockState setPlant(BlockState state, IAgriPlant plant) {
        return PLANT.apply(state, plant);
    }

    public BlockState setGrowthStage(BlockState state, IAgriGrowthStage stage) {
        return GROWTH.apply(state, stage);
    }

    public BlockState setWeed(BlockState state, IAgriWeed weed) {
        return WEED.apply(state, weed);
    }

    public BlockState setWeedGrowthStage(BlockState state, IAgriGrowthStage stage) {
        return WEED_GROWTH.apply(state, stage);
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
        return AgriApi.getFertilizerRegistry().valueOf(BONE_MEAL)
                .flatMap(fertilizer ->
                        AgriApi.getCrop(state, world, pos).map(crop ->
                                crop.acceptsFertilizer(fertilizer)))
                .orElse(false);
    }

    @Override
    public void grow(ServerWorld world, Random rand, BlockPos pos, BlockState state) {
        AgriApi.getFertilizerRegistry().valueOf(BONE_MEAL).ifPresent(fertilizer ->
               AgriApi.getCrop(state, world, pos).ifPresent(crop ->
                        fertilizer.applyFertilizer(crop, BONE_MEAL, rand)));
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
            return AgriApi.getCrop(state, (World) world, pos)
                    .flatMap(crop -> crop.getPlant().flatMap(plant -> plant.asBlockState(crop.getGrowthStage())))
                    .orElse(state);
        }
        return state;
    }
}