package team.chisel.client.render;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

import gnu.trove.set.TLongSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Value;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.common.model.TRSRTransformation;
import team.chisel.Chisel;
import team.chisel.api.render.IChiselFace;
import team.chisel.api.render.IChiselTexture;
import team.chisel.api.render.IModelChisel;
import team.chisel.api.render.RenderContextList;
import team.chisel.client.ChiselExtendedState;
import team.chisel.common.asm.ChiselCoreMethods;
import team.chisel.common.util.ProfileUtil;

/**
 * Model for all chisel blocks
 */
@Deprecated
public class ModelChiselBlockOld extends AbstractChiselBakedModel {

    public ModelChiselBlockOld(@Nonnull IModelChisel model) {
        super(model);
    }
    
    @Override
    protected AbstractChiselBakedModel createModel(IBlockState state, @Nonnull IModelChisel model, RenderContextList ctx) {
        ModelChiselBlockOld ret = new ModelChiselBlockOld(model);
        IBakedModel baked = model.getModel(state);
        List<BakedQuad> quads = Lists.newArrayList();
        for (BlockRenderLayer layer : LAYERS) {
            for (EnumFacing facing : EnumFacing.VALUES) {
                IChiselFace face = model.getFace(facing);
                
                int quadGoal = ctx == null ? 1 : Ordering.natural().max(FluentIterable.from(face.getTextureList()).transform(tex -> tex.getType().getQuadsPerSide()));
                List<BakedQuad> temp = baked.getQuads(state, facing, 0);
                addAllQuads(temp, face, layer, ctx, quadGoal, quads);
                ret.faceQuads.put(layer, facing, ImmutableList.copyOf(quads));

                temp = FluentIterable.from(baked.getQuads(state, null, 0)).filter(q -> q.getFace() == facing).toList();
                addAllQuads(temp, face, layer, ctx, quadGoal, quads);
                ret.genQuads.putAll(layer, quads);
            }
        }
        return ret;
    }

    private void addAllQuads(List<BakedQuad> from, IChiselFace face, BlockRenderLayer layer, @Nullable RenderContextList ctx, int quadGoal, List<BakedQuad> to) {
        to.clear();
        for (BakedQuad q : from) {
            for (IChiselTexture<?> tex : face.getTextureList().stream().filter(t -> t.getLayer() == layer).collect(Collectors.toList())) {
                to.addAll(tex.transformQuad(q, ctx == null ? null : ctx.getRenderContext(tex.getType()), quadGoal));
            }
        }
    }
    
    @Override
    public boolean isAmbientOcclusion() {
        return ((ModelChiselOld)getModel()).ambientOcclusion();
    }
}
