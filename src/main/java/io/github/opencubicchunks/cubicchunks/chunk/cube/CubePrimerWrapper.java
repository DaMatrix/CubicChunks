package io.github.opencubicchunks.cubicchunks.chunk.cube;

import io.github.opencubicchunks.cubicchunks.chunk.biome.CubeBiomeContainer;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.BitSet;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.FluidState;

@SuppressWarnings("deprecation")
public class CubePrimerWrapper extends CubePrimer {

    private final BigCube cube;

    public CubePrimerWrapper(BigCube cubeIn) {
        super(cubeIn.getCubePos(), null, cubeIn.getCubeSections(), null, null);
        this.cube = cubeIn;
    }

    public BigCube getCube()
    {
        return this.cube;
    }

    @Deprecated @Override public ChunkPos getPos() { throw new UnsupportedOperationException("This function should never be called!"); }
    @Override public CubePos getCubePos() {
        return this.cube.getCubePos();
    }

    @Deprecated @Override public LevelChunkSection[] getSections() { throw new UnsupportedOperationException("This should never be called!"); }
    @Override public LevelChunkSection[] getCubeSections() {
        return cube.getCubeSections();
    }

    //STATUS
    @Deprecated @Override public ChunkStatus getStatus() { return this.cube.getCubeStatus(); }
    @Override public ChunkStatus getCubeStatus() {
        return cube.getCubeStatus();
    }

    //BLOCK
    @Override @Nullable public BlockState setBlock(BlockPos pos, BlockState state, boolean isMoving) {
        return null;
    }

    @Override public BlockState getBlockState(int x, int y, int z) {
        return this.cube.getBlockState(x, y, z);
    }

    @Override public FluidState getFluidState(BlockPos pos) {
        return this.cube.getFluidState(pos);
    }

    //ENTITY
    @Deprecated @Override public void addEntity(Entity entityIn) { }
    @Override public void addCubeEntity(Entity entityIn) { }

    //TILE ENTITY
    @Deprecated @Override public void setBlockEntityNbt(CompoundTag nbt) { }
    @Override public void addCubeTileEntity(CompoundTag nbt) { }

    @Deprecated @Override public void removeBlockEntity(BlockPos pos) { }
    @Override public void removeCubeTileEntity(BlockPos pos) { }

    @Deprecated @Override public void setBlockEntity(BlockPos pos, BlockEntity tileEntity) { }
    @Override public void addCubeTileEntity(BlockPos pos, BlockEntity tileEntityIn) { }

    @Override @Nullable public BlockEntity getBlockEntity(BlockPos pos) {
        return this.cube.getBlockEntity(pos);
    }

    @Deprecated @Override @Nullable public CompoundTag getBlockEntityNbtForSaving(BlockPos pos) { return this.getCubeTileEntityNBT(pos); }
    @Override @Nullable public CompoundTag getCubeTileEntityNBT(BlockPos pos) {
        return this.cube.getCubeTileEntityNBT(pos);
    }

    @Deprecated @Override @Nullable public CompoundTag getBlockEntityNbt(BlockPos pos) { return this.getCubeDeferredTileEntity(pos); }
    @Override @Nullable public CompoundTag getCubeDeferredTileEntity(BlockPos pos) {
        return this.cube.getCubeDeferredTileEntity(pos);
    }

    //LIGHTING
    @Deprecated @Override public void setLightCorrect(boolean lightCorrectIn) { throw new UnsupportedOperationException("Chunk method called on a cube!"); }
    @Override public void setCubeLight(boolean lightCorrectIn)
    {
        this.cube.setCubeLight(lightCorrectIn);
    }

    @Deprecated @Override public boolean isLightCorrect() { throw new UnsupportedOperationException("Chunk method called on a cube!"); }
    @Override public boolean hasCubeLight() {
        return this.cube.hasCubeLight();
    }

    @Deprecated @Override public Stream<BlockPos> getLights() { return this.getCubeLightSources(); }
    @Override public Stream<BlockPos> getCubeLightSources() {
        return this.cube.getCubeLightSources();
    }

    @Override public int getMaxLightLevel() {
        return this.cube.getMaxLightLevel();
    }

    //MISC
    @Deprecated @Override public void setUnsaved(boolean modified) { this.setDirty(modified); }
    @Override public void setDirty(boolean modified) {
        this.cube.setDirty(modified);
    }

    @Deprecated @Override public boolean isUnsaved() { return this.isDirty(); }
    @Override public boolean isDirty() {
        return this.cube.isDirty();
    }

    @Override public boolean isEmptyCube() {
        return this.cube.isEmptyCube();
    }

    @Deprecated @Override public ChunkBiomeContainer getBiomes() { return this.getCubeBiomes(); }
    @Override public CubeBiomeContainer getCubeBiomes() {
        return this.cube.getBiomes();
    }

    @Override public void setHeightmap(Heightmap.Types type, long[] data) {
    }

    private Heightmap.Types fixType(Heightmap.Types p_209532_1_) {
        if (p_209532_1_ == Heightmap.Types.WORLD_SURFACE_WG) {
            return Heightmap.Types.WORLD_SURFACE;
        } else {
            return p_209532_1_ == Heightmap.Types.OCEAN_FLOOR_WG ? Heightmap.Types.OCEAN_FLOOR : p_209532_1_;
        }
    }

    @Override public int getHeight(Heightmap.Types heightmapType, int x, int z) {
        return this.cube.getHeight(this.fixType(heightmapType), x, z);
    }

    @Override public void setLastSaveTime(long saveTime) {
    }

    // getStructureStart
    @Override @Nullable public StructureStart<?> getStartForFeature(StructureFeature<?> var1) {
         return this.cube.getStartForFeature(var1);
    }

    @Override public void setStartForFeature(StructureFeature<?> structureIn, StructureStart<?> structureStartIn) {
    }

    @Override public Map<StructureFeature<?>, StructureStart<?>> getAllStarts() {
        return this.cube.getAllStarts();
    }

    @Override public void setAllStarts(Map<StructureFeature<?>, StructureStart<?>> structureStartsIn) {
    }

    @Override public LongSet getReferencesForFeature(StructureFeature<?> structureIn) {
        return this.cube.getReferencesForFeature(structureIn);
    }

    @Override public void addReferenceForFeature(StructureFeature<?> structure, long reference) {
    }

    @Override public Map<StructureFeature<?>, LongSet> getAllReferences() {
        return this.cube.getAllReferences();
    }

    @Override public void setAllReferences(Map<StructureFeature<?>, LongSet> p_201606_1_) {
    }

    @Override public void markPosForPostprocessing(BlockPos pos) {
    }

    /*
    public CubePrimerTickList<Block> getBlocksToBeTicked() {
        return new CubePrimerTickList<>((p_209219_0_) -> {
            return p_209219_0_.getDefaultState().isAir();
        }, this.getPos());
    }

    public CubePrimerTickList<Fluid> getFluidsToBeTicked() {
        return new CubePrimerTickList<>((p_209218_0_) -> {
            return p_209218_0_ == Fluids.EMPTY;
        }, this.getPos());
    }
    */

    @Override public BitSet getCarvingMask(GenerationStep.Carving type) {
        throw Util.pauseInIde(new UnsupportedOperationException("Meaningless in this context"));
    }
    @Override public BitSet setCarvingMask(GenerationStep.Carving type) {
        throw Util.pauseInIde(new UnsupportedOperationException("Meaningless in this context"));
    }
}