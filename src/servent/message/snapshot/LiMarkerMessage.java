package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.SnapshotVersion;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class LiMarkerMessage extends BasicMessage {

    private static final long serialVersionUID = 3916837847384606557L;

    private final SnapshotVersion snapshotVersion;

    public LiMarkerMessage(ServentInfo sender, ServentInfo receiver, SnapshotVersion snapshotVersion){
        super(MessageType.LI_MARKER, sender, receiver, String.valueOf(snapshotVersion.getInit_id()));
        this.snapshotVersion = snapshotVersion;
    }

    public SnapshotVersion getSnapshotVersion() {
        return snapshotVersion;
    }
}
