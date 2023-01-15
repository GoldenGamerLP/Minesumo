package me.alex.minesumo.tasks;

import me.alex.minesumo.instances.ArenaImpl;

import java.util.TimerTask;

public abstract class AbstractTask extends TimerTask {

    protected final ArenaImpl arena;

    protected AbstractTask(ArenaImpl arena) {
        this.arena = arena;
    }

    @Override
    public void run() {
        this.onRun(this.arena);
    }

    abstract void onRun(ArenaImpl arena);

    @Override
    public boolean cancel() {
        this.onStop();
        return super.cancel();
    }

    void onStop() {
    }
}
