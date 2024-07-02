package app.snapshot_bitcake;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LiSnapshotResult implements Serializable {

    private static final long serialVersionUID = 7873607080703717036L;

    private final int serventId;
    private SnapshotVersion snapshotVersion;
    private final int recordedAmount;
    private final Map<Integer, Integer> giveHistory;
    private final Map<Integer, Integer> getHistory;


    public LiSnapshotResult(int serventId ,SnapshotVersion snapshotVersion, int recordedAmount, Map<Integer, Integer> giveHistory, Map<Integer, Integer> getHistory) {
        this.serventId = serventId;
        this.snapshotVersion = snapshotVersion;
        this.recordedAmount = recordedAmount;
        this.giveHistory = new ConcurrentHashMap<>(giveHistory);
        this.getHistory = new ConcurrentHashMap<>(getHistory);
    }

    public int getServentId() {
        return serventId;
    }


    public int getRecordedAmount() {
        return recordedAmount;
    }

    public Map<Integer, Integer> getGiveHistory() {
        return giveHistory;
    }

    public Map<Integer, Integer> getGetHistory() {
        return getHistory;
    }

    public SnapshotVersion getSnapshotVersion() {
        return snapshotVersion;
    }
}
