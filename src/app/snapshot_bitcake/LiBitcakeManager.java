package app.snapshot_bitcake;

import app.AppConfig;
import servent.message.Message;
import servent.message.snapshot.LYTellMessage;
import servent.message.snapshot.LiMarkerMessage;
import servent.message.snapshot.LiTellMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class LiBitcakeManager implements BitcakeManager{


    private final AtomicInteger currentAmount = new AtomicInteger(1000);

    private Map<Integer, Integer> giveHistory = new ConcurrentHashMap<>();
    private Map<Integer, Integer> getHistory = new ConcurrentHashMap<>();

    public int recordedAmount = 0;

    public LiBitcakeManager(){
        for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()){
            giveHistory.put(neighbor, 0);
            getHistory.put(neighbor, 0);
        }
    }

    @Override
    public void takeSomeBitcakes(int amount) {
        currentAmount.getAndAdd(-amount);
    }

    @Override
    public void addSomeBitcakes(int amount) {
        currentAmount.getAndAdd(amount);
    }

    @Override
    public int getCurrentBitcakeAmount() {
        synchronized (AppConfig.LiLOCK){
            return currentAmount.get();
        }
    }

//    public void markerEvent(int collectorId, SnapshotCollector snapshotCollector){
//        synchronized (AppConfig.colorLock){
//            AppConfig.isWhite.set(false);
//            recordedAmount = getCurrentBitcakeAmount();
//
//            LiSnapshotResult snapshotResult = new LiSnapshotResult(
//                    AppConfig.myServentInfo.getId(), recordedAmount, giveHistory, getHistory);
//
//            if(collectorId == AppConfig.myServentInfo.getId()){
//                snapshotCollector.addLiSnapshotInfo(AppConfig.myServentInfo.getId(), snapshotResult);
//            }else{
//                Message tellMessage = new LiTellMessage(AppConfig.myServentInfo, AppConfig.getInfoById(collectorId), snapshotResult);
//
//                MessageUtil.sendMessage(tellMessage);
//            }
//
//            for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()){
//                Message liMarker = new LiMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId);
//                MessageUtil.sendMessage(liMarker);
//
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//
//        }
//    }

    public void markerEvent(SnapshotVersion snapshotVersion, SnapshotCollector snapshotCollector){
        synchronized (AppConfig.LiLOCK){
            if(snapshotVersion.getInit_id().get() == AppConfig.myServentInfo.getId()){
                if(!AppConfig.initIsSet.get()){
                    AppConfig.initIsSet.set(true);
                }else{
                    return;
                }
            }else{
                if(!AppConfig.alreadyGotMarker(snapshotVersion)){
                    AppConfig.addToGotMarkerList(snapshotVersion);
                }else{
                    return;
                }
            }

            //treba napraviti deep copy za give i get
            Map<Integer, Integer> giveCopy = copyGiveHistory();
            Map<Integer, Integer> getCopy = copyGetHistory();


            recordedAmount = getCurrentBitcakeAmount();
            LiSnapshotResult snapshotResult = new LiSnapshotResult(AppConfig.myServentInfo.getId(), snapshotVersion,
                    recordedAmount, giveHistory, getHistory);

//            int sum = summing(recordedAmount);
//            recordedAmount = getCurrentBitcakeAmount();
//            LiSnapshotResult snapshotResult = new LiSnapshotResult(AppConfig.myServentInfo.getId(), snapshotVersion,
//                    recordedAmount, giveCopy, getCopy);
//            AppConfig.timestampedStandardPrint("R + Gv + Gt: " + sum + ", for snapshot_no: " + snapshotVersion.getSnapshot_no());

            printNodeState(snapshotResult);

            if(snapshotVersion.getInit_id().get() == AppConfig.myServentInfo.getId()){
                snapshotCollector.addLiSnapshotInfo(AppConfig.myServentInfo.getId(), snapshotResult);
            }else{
                Message tellMessage = new LiTellMessage(AppConfig.myServentInfo, AppConfig.getInfoById(snapshotVersion.getInit_id().get()), snapshotResult);
                MessageUtil.sendMessage(tellMessage);
            }

            for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()){
                Message liMarker = new LiMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), snapshotVersion);
                MessageUtil.sendMessage(liMarker);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private class MapValueUpdater implements BiFunction<Integer, Integer, Integer>{

        private int valueToAdd;

        public MapValueUpdater(int valueToAdd) {
            this.valueToAdd = valueToAdd;
        }

        @Override
        public Integer apply(Integer key, Integer odlValue) {
            return odlValue + valueToAdd;
        }
    }

    public void recordGiveTransaction(int neighbor, int amount){
        synchronized (AppConfig.LiLOCK){
            giveHistory.compute(neighbor, new MapValueUpdater(amount));
        }
    }

    public void recordGetTransaction(int neighbor, int amount){
        synchronized (AppConfig.LiLOCK){
            getHistory.compute(neighbor, new MapValueUpdater(amount));
        }
    }

    public Map<Integer, Integer> copyGiveHistory() {
        Map<Integer, Integer> copiedGiveHistory = new ConcurrentHashMap<>();
        copiedGiveHistory.putAll(giveHistory);
        return copiedGiveHistory;
    }

    // Metoda za pravljenje deep copy mape getHistory
    public Map<Integer, Integer> copyGetHistory() {
        Map<Integer, Integer> copiedGetHistory = new ConcurrentHashMap<>();
        copiedGetHistory.putAll(getHistory);
        return copiedGetHistory;
    }



    private void printNodeState(LiSnapshotResult liSnapshotResult){
        int sum = 0;
        sum += liSnapshotResult.getRecordedAmount();

        for (Map.Entry<Integer, Integer> entry : liSnapshotResult.getGiveHistory().entrySet()){
            sum += entry.getValue();
        }

        for (Map.Entry<Integer, Integer> entry : liSnapshotResult.getGetHistory().entrySet()){
            sum -= entry.getValue();
        }

        AppConfig.timestampedStandardPrint("State for node " + AppConfig.myServentInfo.getId() + ": " + sum);
    }

}

