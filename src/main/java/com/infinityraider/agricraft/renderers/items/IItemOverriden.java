/*
 */
package com.infinityraider.agricraft.renderers.items;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 *
 * @author RlonRyan
 */
@FunctionalInterface
public interface IItemOverriden {

	BakedAgriItemModel handleItemState(IBakedModel originalModel, ItemStack stack, World world, EntityLivingBase entity);

	public static final class Wrapper extends ItemOverrideList {

		public final IItemOverriden override;

		public Wrapper(IItemOverriden override) {
			super(ImmutableList.of());
			this.override = override;
		}

		@Override
		public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, World world, EntityLivingBase entity) {
			return this.override.handleItemState(originalModel, stack, world, entity);
		}

	}

}
