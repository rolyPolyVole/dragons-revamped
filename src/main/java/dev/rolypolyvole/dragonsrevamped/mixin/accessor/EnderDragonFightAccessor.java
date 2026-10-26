package dev.rolypolyvole.dragonsrevamped.mixin.accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnderDragonFight.class)
public interface EnderDragonFightAccessor {

    @Accessor("needsStateScanning")
    boolean needsStateScanning();

    @Accessor("exitPortalLocation")
    BlockPos exitPortalLocation();
}
