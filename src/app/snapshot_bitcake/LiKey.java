package app.snapshot_bitcake;

import java.util.concurrent.atomic.AtomicInteger;

public class LiKey {
    private AtomicInteger processId;
    private AtomicInteger snapshotVersion;


    public LiKey(AtomicInteger processId, AtomicInteger snapshotVersion) {
        this.processId = processId;
        this.snapshotVersion = snapshotVersion;
    }

    public AtomicInteger getProcessId() {
        return processId;
    }

    public AtomicInteger getSnapshotVersion() {
        return snapshotVersion;
    }
}
