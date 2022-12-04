package me.alex.minesumo.listener;

import me.alex.minesumo.data.instances.Arena;
import me.alex.minesumo.events.PlayerArenaDeathEvent;
import me.alex.minesumo.events.PlayerJoinArenaEvent;
import me.alex.minesumo.events.PlayerLeaveArenaEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerMoveEvent;

public class GlobalEventListener {

    public GlobalEventListener() {
        GlobalEventHandler gl = MinecraftServer.getGlobalEventHandler();

        gl.addListener(RemoveEntityFromInstanceEvent.class,removeEntityFromInstanceEvent -> {
           if(removeEntityFromInstanceEvent.getInstance() instanceof Arena arena
                   && removeEntityFromInstanceEvent.getEntity() instanceof Player player) {
               gl.call(new PlayerLeaveArenaEvent(arena, player));
           }
        });

        gl.addListener(AddEntityToInstanceEvent.class,addEntityToInstanceEvent -> {
           if(addEntityToInstanceEvent.getInstance() instanceof Arena arena
                   && addEntityToInstanceEvent.getEntity() instanceof Player player) {
               PlayerJoinArenaEvent event = new PlayerJoinArenaEvent(arena, player);
               gl.call(event);
               addEntityToInstanceEvent.setCancelled(event.isCancelled());
           }
        });

        gl.addListener(PlayerMoveEvent.class,playerMoveEvent -> {
            if(!(playerMoveEvent.getInstance() instanceof Arena arena)) return;
            if(!(playerMoveEvent.getNewPosition().y() > arena.getMapConfig().getDeathPositions().y())) return;

            PlayerArenaDeathEvent death = new PlayerArenaDeathEvent(playerMoveEvent.getPlayer(),arena);
            gl.call(death);

            if(death.getNewPlayerPosition() != null) playerMoveEvent.setNewPosition(death.getNewPlayerPosition());
        });
    }
}
