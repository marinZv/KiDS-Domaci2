package app.snapshot_bitcake;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class SnapshotVersion implements Serializable {

    private static final long serialVersionUID = 6857165363063006682L;

    private AtomicInteger init_id;
    private AtomicInteger snapshot_no;

    public SnapshotVersion(int id, int version){
        init_id = new AtomicInteger(id);
        snapshot_no = new AtomicInteger(version);
    }

    public AtomicInteger getInit_id() {
        return init_id;
    }

    public AtomicInteger getSnapshot_no() {
        return snapshot_no;
    }
}
