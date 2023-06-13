package teleport_altar;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.function.Function;

public class DirectTeleporter implements ITeleporter {

    private final PortalInfo portalInfo;

    public DirectTeleporter(final Vec3 targetVec, final Vec3 targetSpeed, final float targetYRot, final float targetXRot) {
        this.portalInfo = new PortalInfo(targetVec, targetSpeed, targetYRot, targetXRot);
    }

    public static DirectTeleporter create(final Entity entity, final Vec3 targetVec) {
        return new DirectTeleporter(targetVec, Vec3.ZERO, entity.getYRot(), entity.getXRot());
    }

    @Nullable
    @Override
    public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
        return portalInfo;
    }

    @Override
    public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceWorld, ServerLevel destWorld) {
        return false;
    }

    @Override
    public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
        Vec3 targetVec = portalInfo.pos;
        Vec3 targetMotion = portalInfo.speed;
        float targetRot = portalInfo.yRot;

        entity.setDeltaMovement(targetMotion);

        if (entity instanceof ServerPlayer) {
            ((ServerPlayer) entity).connection.teleport(targetVec.x(), targetVec.y(), targetVec.z(), targetRot, entity.getXRot());
        } else {
            entity.moveTo(targetVec.x(), targetVec.y(), targetVec.z(), targetRot, entity.getXRot());
        }

        return repositionEntity.apply(false);
    }
}
