package me.alex.minesumo.data.tasks;

import me.alex.minesumo.instances.ArenaImpl;

import java.util.TimerTask;

public abstract class AbstractTask extends TimerTask {

    private final ArenaImpl arena;

    protected AbstractTask(ArenaImpl arena) {
        this.arena = arena;
    }

    @Override
    public void run() {
        this.onRun(this.arena);
    }

    abstract void onRun(ArenaImpl arena);
}
