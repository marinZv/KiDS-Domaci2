package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.LiSnapshotResult;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.util.List;

public class LiTellMessage extends BasicMessage {

    private static final long serialVersionUID = -5198842552104735274L;

    private LiSnapshotResult liSnapshotResult;

    public LiTellMessage(ServentInfo sender, ServentInfo receiver, LiSnapshotResult liSnapshotResult){
        super(MessageType.LI_TELL, sender, receiver);
        this.liSnapshotResult = liSnapshotResult;
    }

    private LiTellMessage(MessageType messageType, ServentInfo sender, ServentInfo receiver,
                          boolean white, List<ServentInfo> routeList, String messageText, int messageId,
                          LiSnapshotResult liSnapshotResult){
        super(messageType, sender, receiver, white, routeList, messageText, messageId);
        this.liSnapshotResult = liSnapshotResult;
    }

    public LiSnapshotResult getLiSnapshotResult() {
        return liSnapshotResult;
    }

    @Override
    public Message setRedColor() {
        Message toReturn = new LiTellMessage(getMessageType(), getOriginalSenderInfo(), getReceiverInfo(),
                false, getRoute(), getMessageText(), getMessageId(), getLiSnapshotResult());
        return toReturn;
    }
}
