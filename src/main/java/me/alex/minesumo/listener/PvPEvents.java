package me.alex.minesumo.listener;

import io.github.bloepiloepi.pvp.legacy.LegacyKnockbackSettings;
import me.alex.minesumo.Minesumo;
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


            target.setTag(LAST_HIT, attacker.getUuid());
            target.setTag(isHitable, true);
            target.scheduler().buildTask(() -> target.removeTag(isHitable)).delay(TaskSchedule.millis(325)).schedule();

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
        double dx = attacker.getPosition().x() - entity.getPosition().x();

        double dz = attacker.getPosition().z() - entity.getPosition().z();
        for (; dx * dx + dz * dz < 0.0002; dz = (Math.random() - Math.random()) * 0.01D) {
            dx = (Math.random() - Math.random()) * 0.02D;
        }

        double finalDx = dx;
        double finalI = dz;
        double magnitude = Math.sqrt(dx * dx + dz * dz);

        Vec newVelocity = entity.getVelocity();

        double horizontal = settings.horizontal();
        newVelocity = newVelocity.withX((newVelocity.x() / 1.7) - (finalDx / magnitude * horizontal));
        newVelocity = newVelocity.withY((newVelocity.y() / 1.65) + settings.vertical());
        newVelocity = newVelocity.withZ((newVelocity.z() / 1.7) - (finalI / magnitude * horizontal));

        if (newVelocity.y() > settings.verticalLimit())
            newVelocity = newVelocity.withY(settings.verticalLimit());


        entity.setVelocity(newVelocity);
    }

}
