package dev.rolypolyvole.dragonsrevamped.worldgen;

import dev.rolypolyvole.dragonsrevamped.DragonsRevamped;
import dev.rolypolyvole.dragonsrevamped.mixin.accessor.EnderDragonFightAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class CustomEndSpikes {
    private CustomEndSpikes() {}

    private static final float MAX_LEAN_PER_Y = 0.35f;
    private static final float TAPER_EXPONENT = 0.97f;
    private static final int TIP_STRUCTURE_HEIGHT = 6;
    private static final int ARENA_CHUNK_RADIUS = 8;

    public static List<Vec3> getCrystalLocations(ServerLevel level) {
        return EndSpikeFeature.getSpikesForLevel(level).stream()
            .map(spike -> new Vec3(spike.getCenterX() + 0.5, spike.getHeight(), spike.getCenterZ() + 0.5))
            .toList();
    }

    // ── Spike placement ─────────────────────────────────────────────────

    public static void place(ServerLevelAccessor level, EndSpikeFeature feature, EndSpikeFeature.EndSpike spike) {
        if (level instanceof WorldGenRegion) return;

        placeSpike(level, spike, feature.crystalInvulnerable(), feature.crystalBeamTarget().orElse(null));
    }

    public static void placeInitialSpikes(ServerLevel end) {
        EnderDragonFight fight = end.getDragonFight();
        if (fight == null || !((EnderDragonFightAccessor) fight).dragonsrevamped$needsStateScanning()) return;

        for (int x = -ARENA_CHUNK_RADIUS; x <= ARENA_CHUNK_RADIUS; x++) {
            for (int z = -ARENA_CHUNK_RADIUS; z <= ARENA_CHUNK_RADIUS; z++) {
                end.getChunk(x, z, ChunkStatus.FULL, true);
            }
        }

        int placed = 0;

        for (EndSpikeFeature.EndSpike spike : EndSpikeFeature.getSpikesForLevel(end)) {
            if (isPlaced(end, spike)) continue;

            placeSpike(end, spike, false, null);
            placed++;
        }

        if (placed > 0) {
            DragonsRevamped.LOGGER.info("Placed {} End spikes", placed);
        }
    }

    private static boolean isPlaced(ServerLevel level, EndSpikeFeature.EndSpike spike) {
        BlockPos crystalBase = new BlockPos(spike.getCenterX(), spike.getHeight() - 1, spike.getCenterZ());
        return level.getBlockState(crystalBase).is(Blocks.BEDROCK);
    }

    private static void placeSpike(
            ServerLevelAccessor level,
            EndSpikeFeature.EndSpike spike,
            boolean crystalInvulnerable,
            @Nullable BlockPos crystalBeamTarget
    ) {
        RandomSource random = spikeRandom(level, spike);

        int tipX = spike.getCenterX();
        int tipZ = spike.getCenterZ();
        int crystalBlockY = spike.getHeight();
        int tipBaseY = crystalBlockY - TIP_STRUCTURE_HEIGHT;

        float[] lean = outwardLean(tipX, tipZ, random);
        float leanX = lean[0];
        float leanZ = lean[1];

        // Deterministic startY — no terrain scanning, so spikes are identical on respawn
        int startY = Math.max(level.getMinY(), crystalBlockY - TIP_STRUCTURE_HEIGHT - 80);

        int bodyHeight = Math.max(1, tipBaseY - startY);
        int baseRadius = Mth.clamp(bodyHeight / 10 + 2, 4, 9);

        // Base center at startY (shifted from tip by lean)
        float baseX = tipX - (tipBaseY - startY) * leanX;
        float baseZ = tipZ - (tipBaseY - startY) * leanZ;

        placeIsland(level, baseX, baseZ, startY, baseRadius + 2);
        placeSpire(level, random, tipX, tipZ, startY, tipBaseY, baseRadius, leanX, leanZ);
        placeTipStructure(level, tipX, tipBaseY, tipZ);

        if (spike.isGuarded()) {
            placeCage(level, tipX, crystalBlockY, tipZ, 4);
        }

        spawnCrystal(level, random, tipX, crystalBlockY, tipZ, crystalInvulnerable, crystalBeamTarget);
    }

    // ── Base island ────────────────────────────────────────────────────

    private static void placeIsland(ServerLevelAccessor level, float centerX, float centerZ, int topY, int topRadius) {
        MutableBlockPos pos = new MutableBlockPos();

        for (int dy = 0; dy <= topRadius; dy++) {
            int y = topY - dy;
            float t = dy / (float) topRadius;

            // Hemisphere profile: radius shrinks as we go down
            float radius = topRadius * (float) Math.sqrt(1.0f - t * t);
            if (radius < 0.5f) break;

            fillDisc(level, pos, centerX, centerZ, y, radius, Blocks.END_STONE.defaultBlockState());
        }
    }

    private static void fillDisc(ServerLevelAccessor level, MutableBlockPos pos, float centerX, float centerZ, int y, float radius, BlockState state) {
        float radiusSqr = radius * radius;
        int minX = (int) Math.floor(centerX - radius - 1);
        int maxX = (int) Math.ceil(centerX + radius + 1);
        int minZ = (int) Math.floor(centerZ - radius - 1);
        int maxZ = (int) Math.ceil(centerZ + radius + 1);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                float dx = (x + 0.5f) - centerX;
                float dz = (z + 0.5f) - centerZ;

                if (dx * dx + dz * dz <= radiusSqr) {
                    pos.set(x, y, z);
                    level.setBlock(pos, state, 2);
                }
            }
        }
    }

    // ── Spike body ──────────────────────────────────────────────────────

    private static void placeSpire(
            ServerLevelAccessor level,
            RandomSource random,
            int tipX,
            int tipZ,
            int startY,
            int tipBaseY,
            int baseRadius,
            float leanX,
            float leanZ
    ) {
        MutableBlockPos pos = new MutableBlockPos();
        int bodyHeight = Math.max(1, tipBaseY - startY);

        for (int y = startY; y <= tipBaseY; y++) {
            float t = (y - startY) / (float) bodyHeight;

            // Lean: tip stays at (tipX, tipZ), base shifts inward toward origin
            float centerX = tipX - (tipBaseY - y) * leanX;
            float centerZ = tipZ - (tipBaseY - y) * leanZ;

            // Taper: gradually narrows from baseRadius at bottom to single-block at top
            float radius = 0.5f + (baseRadius - 0.5f) * (float) Math.pow(1.0f - t, TAPER_EXPONENT);

            // Always place the spine (center) block
            pos.set(Mth.floor(centerX), y, Mth.floor(centerZ));
            level.setBlock(pos, pickSpikeBlock(random, y, tipBaseY), 2);

            if (radius <= 1.0f) continue;

            float radiusSqr = radius * radius;
            int minX = (int) Math.floor(centerX - radius - 1);
            int maxX = (int) Math.ceil(centerX + radius + 1);
            int minZ = (int) Math.floor(centerZ - radius - 1);
            int maxZ = (int) Math.ceil(centerZ + radius + 1);

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    float dx = (x + 0.5f) - centerX;
                    float dz = (z + 0.5f) - centerZ;

                    if (dx * dx + dz * dz <= radiusSqr) {
                        pos.set(x, y, z);
                        level.setBlock(pos, pickSpikeBlock(random, y, tipBaseY), 2);
                    }
                }
            }
        }
    }

    // ── Tip structure ───────────────────────────────────────────────────

    private static void placeTipStructure(ServerLevelAccessor level, int x, int tipBaseY, int z) {
        MutableBlockPos pos = new MutableBlockPos();

        // Center column
        setBlock(level, pos, x, tipBaseY + 1, z, Blocks.OBSIDIAN.defaultBlockState());
        setBlock(level, pos, x, tipBaseY + 2, z, Blocks.OBSIDIAN.defaultBlockState());
        setBlock(level, pos, x, tipBaseY + 3, z, Blocks.CRYING_OBSIDIAN.defaultBlockState());
        setBlock(level, pos, x, tipBaseY + 4, z, Blocks.REINFORCED_DEEPSLATE.defaultBlockState());
        setBlock(level, pos, x, tipBaseY + 5, z, Blocks.BEDROCK.defaultBlockState());
        setBlock(level, pos, x, tipBaseY + 6, z, Blocks.SOUL_FIRE.defaultBlockState());

        // Cardinal sides of reinforced deepslate: upside-down stairs + candles above
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            int sideX = x + direction.getStepX();
            int sideZ = z + direction.getStepZ();

            BlockState stair = Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, direction.getOpposite())
                .setValue(BlockStateProperties.HALF, Half.TOP);
            setBlock(level, pos, sideX, tipBaseY + 4, sideZ, stair);

            BlockState candle = Blocks.DYED_CANDLE.black().defaultBlockState()
                .setValue(BlockStateProperties.LIT, false)
                .setValue(BlockStateProperties.CANDLES, 4);
            setBlock(level, pos, sideX, tipBaseY + 5, sideZ, candle);
        }

        // Diagonal corners: upper slab + lower slab above
        int[][] diagonals = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] diagonal : diagonals) {
            int cornerX = x + diagonal[0];
            int cornerZ = z + diagonal[1];

            BlockState upperSlab = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState()
                .setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP);
            setBlock(level, pos, cornerX, tipBaseY + 4, cornerZ, upperSlab);

            BlockState lowerSlab = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState()
                .setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
            setBlock(level, pos, cornerX, tipBaseY + 5, cornerZ, lowerSlab);
        }
    }

    // ── Iron bar cage ────────────────────────────────────────────────────

    private static void placeCage(ServerLevelAccessor level, int centerX, int centerY, int centerZ, int radius) {
        MutableBlockPos pos = new MutableBlockPos();
        float innerRadiusSqr = (radius - 0.5f) * (radius - 0.5f);
        float outerRadiusSqr = (radius + 0.5f) * (radius + 0.5f);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    float distanceSqr = dx * dx + dy * dy + dz * dz;

                    if (distanceSqr >= innerRadiusSqr && distanceSqr <= outerRadiusSqr) {
                        pos.set(centerX + dx, centerY + dy, centerZ + dz);

                        if (level.getBlockState(pos).isAir()) {
                            level.setBlock(pos, Blocks.IRON_BARS.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    // ── Crystal spawning ────────────────────────────────────────────────

    private static void spawnCrystal(
            ServerLevelAccessor level,
            RandomSource random,
            int x,
            int crystalBlockY,
            int z,
            boolean invulnerable,
            @Nullable BlockPos beamTarget
    ) {
        EndCrystal crystal = EntityTypes.END_CRYSTAL.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (crystal == null) return;

        crystal.setBeamTarget(beamTarget);
        crystal.setPermanentlyInvulnerable(invulnerable);
        crystal.setShowBottom(false);
        crystal.snapTo(x + 0.5, crystalBlockY, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(crystal);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static RandomSource spikeRandom(ServerLevelAccessor level, EndSpikeFeature.EndSpike spike) {
        long worldSeed = level.getLevel().getSeed();
        long x = spike.getCenterX();
        long z = spike.getCenterZ();
        long seed = worldSeed
            ^ (x * 341873128712L)
            ^ (z * 132897987541L)
            ^ (spike.isGuarded() ? 0x9E3779B97F4A7C15L : 0L);
        return RandomSource.create(seed);
    }

    private static float[] outwardLean(int x, int z, RandomSource random) {
        float length = Mth.sqrt((float) (x * x + z * z));
        float normalX = length == 0 ? 0 : x / length;
        float normalZ = length == 0 ? 0 : z / length;

        float leanX = normalX * MAX_LEAN_PER_Y;
        float leanZ = normalZ * MAX_LEAN_PER_Y;

        float wobble = (random.nextFloat() - 0.5f) * (MAX_LEAN_PER_Y * 0.25f);
        leanX += -normalZ * wobble;
        leanZ += normalX * wobble;

        return new float[]{leanX, leanZ};
    }

    private static BlockState pickSpikeBlock(RandomSource random, int y, int topY) {
        if (random.nextFloat() < 0.2f * y / topY) return Blocks.CRYING_OBSIDIAN.defaultBlockState();
        return Blocks.OBSIDIAN.defaultBlockState();
    }

    private static void setBlock(ServerLevelAccessor level, MutableBlockPos pos, int x, int y, int z, BlockState state) {
        pos.set(x, y, z);
        level.setBlock(pos, state, 2);
    }
}
