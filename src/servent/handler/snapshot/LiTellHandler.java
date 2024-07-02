package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.LiTellMessage;

public class LiTellHandler implements MessageHandler {

    private Message clientMessage;
    private SnapshotCollector snapshotCollector;

    public LiTellHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        synchronized (AppConfig.LiLOCK){
            if(clientMessage.getMessageType() == MessageType.LI_TELL){
                LiTellMessage liTellMessage = (LiTellMessage) clientMessage;

                snapshotCollector.addLiSnapshotInfo(liTellMessage.getOriginalSenderInfo().getId(),
                        liTellMessage.getLiSnapshotResult());
            }
        }
    }
}
