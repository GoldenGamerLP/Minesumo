package me.alex.minesumo.listener;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.utils.LegacyKnockbackSettings;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.EntityAnimationPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PvPEvents {

    public static final Tag<Integer> TEAM_TAG = Tag.Integer("minsumo_team");
    public static final Tag<UUID> LAST_HIT = Tag.UUID("minsumo_last_hit");
    private static final Tag<Boolean> isHitable = Tag.Boolean("isHitable");

    private static final LegacyKnockbackSettings settings = LegacyKnockbackSettings.builder()
            .horizontal(0.43)
            .vertical(0.36)
            .verticalLimit(0.4308)
            .extraHorizontal(0.6)
            .extraVertical(0.15)
            .build();

    static {

        MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent.class, event -> {
            Entity attacker = event.getEntity();
            Entity target = event.getTarget();

            if (!attacker.hasTag(TEAM_TAG) && !target.hasTag(TEAM_TAG)) return;
            if (Objects.equals(attacker.getTag(TEAM_TAG), target.getTag(TEAM_TAG))) return;
            if (target.hasTag(isHitable)) return;
            if (!(attacker.getInstance() instanceof ArenaImpl impl)) return;
            if (impl.getState() != ArenaImpl.ArenaState.INGAME) return;

            target.setTag(LAST_HIT, attacker.getUuid());
            target.setTag(isHitable, true);
            target.scheduler().buildTask(() -> target.removeTag(isHitable)).delay(TaskSchedule.millis(425)).schedule();

            Sound sound = Sound.sound(Key.key("minecraft:entity.player.hurt"), Sound.Source.MASTER, 1F, 1F);
            ServerPacket ani = new EntityAnimationPacket(
                    target.getEntityId(),
                    EntityAnimationPacket.Animation.TAKE_DAMAGE);

            performAttack(attacker, target);
            target.getInstance().playSound(sound, target.getPosition());
            target.sendPacketToViewersAndSelf(ani);
        });
    }

    public PvPEvents(Minesumo minsumo) {

    }

    public static void performAttack(Entity attacker, Entity entity) {
        //Optimize this method

        double extraHorizontal = .1d * MinecraftServer.TICK_PER_SECOND;
        double dx = attacker.getPosition().x() - entity.getPosition().x();
        double dz = attacker.getPosition().z() - entity.getPosition().z();

        // Randomize direction
        ThreadLocalRandom random = ThreadLocalRandom.current();
        while (dx * dx + dz * dz < 0.0001) {
            dx = random.nextDouble(-1, 1) * 0.01;
            dz = random.nextDouble(-1, 1) * 0.01;
        }

        double finalDx = dx;
        double finalDz = dz;

        float kbResistance = 0;
        double horizontal = settings.horizontal() * (1 - kbResistance) + extraHorizontal;
        double vertical = settings.vertical() * (1 - kbResistance);
        Vec horizontalModifier = new Vec(finalDx, finalDz).normalize().mul(horizontal);


        Vec velocity = entity.getVelocity();
        entity.setVelocity(new Vec(
                velocity.x() / 2d - horizontalModifier.x(),
                entity.isOnGround() ? Math.min(
                        settings.verticalLimit(), velocity.y() + vertical + settings.extraVertical()) : velocity.y(),
                velocity.z() / 2d - horizontalModifier.z()
        ));
    }

}
