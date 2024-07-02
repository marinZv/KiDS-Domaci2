package app.snapshot_bitcake;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import app.AppConfig;
import servent.message.MessageType;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	
	private AtomicBoolean collecting = new AtomicBoolean(false);
	
	private Map<Integer, LYSnapshotResult> collectedLYValues = new ConcurrentHashMap<>();
	
	private BitcakeManager bitcakeManager;

	private SnapshotType snapshotType;

	private Map<LiKey, LiSnapshotResult> collectedLiValues = new ConcurrentHashMap<>();

	public SnapshotCollectorWorker(SnapshotType snapshotType) {
		this.snapshotType = snapshotType;
		switch (snapshotType){
			case LAI_YANG :
				bitcakeManager = new LaiYangBitcakeManager();
				break;
			case LI:
				bitcakeManager = new LiBitcakeManager();
				break;
		}
	}
	
	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}
	
	@Override
	public void run() {
		while(working) {
			
			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (collecting.get() == false) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */

			SnapshotVersion snapshotVersion = new SnapshotVersion(AppConfig.myServentInfo.getId(), AppConfig.getSnapshotVersion());
			AppConfig.initIsSet.set(false);
			//1 send asks
			switch (snapshotType){
				case LAI_YANG :
					((LaiYangBitcakeManager)bitcakeManager).markerEvent(AppConfig.myServentInfo.getId(), this);
					break;
				case LI:
					AppConfig.timestampedStandardPrint("Started collecting, snapshot: " + snapshotVersion.getInit_id() + ", " + snapshotVersion.getSnapshot_no());
					((LiBitcakeManager)bitcakeManager).markerEvent(snapshotVersion, this);
					break;

			}



			//2 wait for responses or finish
			boolean waiting = true;
			while (waiting) {
				if(snapshotType == SnapshotType.LAI_YANG){
					if (collectedLYValues.size() == AppConfig.getServentCount()) {
						waiting = false;
					}
				}else if(snapshotType == SnapshotType.LI){
					if(isCollectingDone(snapshotVersion.getSnapshot_no().get())){
						waiting = false;
					}
				}

				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}

//			printLaiYang();
//			testPrintCollectedLi();
			printLi(snapshotVersion);
//			printLiVersionTwo(snapshotVersion);

			//print
//			int sum;
//			sum = 0;
//			for (Entry<Integer, LYSnapshotResult> nodeResult : collectedLYValues.entrySet()) {
//				sum += nodeResult.getValue().getRecordedAmount();
//				AppConfig.timestampedStandardPrint(
//						"Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().getRecordedAmount());
//			}
//			for(int i = 0; i < AppConfig.getServentCount(); i++) {
//				for (int j = 0; j < AppConfig.getServentCount(); j++) {
//					if (i != j) {
//						if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
//							AppConfig.getInfoById(j).getNeighbors().contains(i)) {
//							int ijAmount = collectedLYValues.get(i).getGiveHistory().get(j);
//							int jiAmount = collectedLYValues.get(j).getGetHistory().get(i);
//
//							if (ijAmount != jiAmount) {
//								String outputString = String.format(
//										"Unreceived bitcake amount: %d from servent %d to servent %d",
//										ijAmount - jiAmount, i, j);
//								AppConfig.timestampedStandardPrint(outputString);
//								sum += ijAmount - jiAmount;
//							}
//						}
//					}
//				}
//			}
//
//			AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
//
//			collectedLYValues.clear(); //reset for next invocation
//			collecting.set(false);
		}

	}
	
	@Override
	public void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult) {
		collectedLYValues.put(id, lySnapshotResult);
	}

	@Override
	public void addLiSnapshotInfo(int id, LiSnapshotResult liSnapshotResult) {
		synchronized (AppConfig.LiLOCK){
			LiKey key = new LiKey(new AtomicInteger(id), new AtomicInteger(liSnapshotResult.getSnapshotVersion().getSnapshot_no().get()));
			collectedLiValues.put(key, liSnapshotResult);

			AppConfig.timestampedStandardPrint("Ubacio sam result u collectedLi " + summing(liSnapshotResult) + " za id:" + id);
		}
	}

	@Override
	public void startCollecting() {
		boolean oldValue = this.collecting.getAndSet(true);
		
		if (oldValue == true) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}

	public boolean isCollectingDone(int version){
		synchronized (AppConfig.LiLOCK){
//			AppConfig.timestampedStandardPrint("");
			AtomicInteger counter = new AtomicInteger(0);
			for (LiKey key : collectedLiValues.keySet()){
				AtomicInteger snapshotVersion = key.getSnapshotVersion();

				if(snapshotVersion.get() == version){
					counter.getAndIncrement();
				}

			}

			if(counter.get() == AppConfig.getServentCount())
				return true;

			return false;

		}
	}
	
	@Override
	public void stop() {
		working = false;
	}

	private void printLaiYang(){
		int sum;
		sum = 0;
		for (Entry<Integer, LYSnapshotResult> nodeResult : collectedLYValues.entrySet()) {
			sum += nodeResult.getValue().getRecordedAmount();
			AppConfig.timestampedStandardPrint(
					"Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().getRecordedAmount());
		}
		for(int i = 0; i < AppConfig.getServentCount(); i++) {
			for (int j = 0; j < AppConfig.getServentCount(); j++) {
				if (i != j) {
					if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
							AppConfig.getInfoById(j).getNeighbors().contains(i)) {
						int ijAmount = collectedLYValues.get(i).getGiveHistory().get(j);
						int jiAmount = collectedLYValues.get(j).getGetHistory().get(i);

						if (ijAmount != jiAmount) {
							String outputString = String.format(
									"Unreceived bitcake amount: %d from servent %d to servent %d",
									ijAmount - jiAmount, i, j);
							AppConfig.timestampedStandardPrint(outputString);
							sum += ijAmount - jiAmount;
						}
					}
				}
			}
		}

		AppConfig.timestampedStandardPrint("System bitcake count: " + sum);

		collectedLYValues.clear(); //reset for next invocation
		collecting.set(false);
	}

	private void printLi(SnapshotVersion snapshotVersion){
		/*TODO:Treba proci kroz collectedLiValues i za prosledjenu verziju snapshota izracunati
			   // stanje svih cvorova kao i stanje svih kanala i ispisati to
		* */


		synchronized (AppConfig.LiLOCK){
			int sum = 0;
			for(Entry<LiKey, LiSnapshotResult> nodeResult : collectedLiValues.entrySet()){
				if(nodeResult.getKey().getSnapshotVersion().get() ==snapshotVersion.getSnapshot_no().get()){
					sum += nodeResult.getValue().getRecordedAmount();
					AppConfig.timestampedStandardPrint(
							"Recorded bitcake amount for " + nodeResult.getKey().getProcessId() + " = "
									+ nodeResult.getValue().getRecordedAmount());
				}
			}


		for (int i = 0; i < AppConfig.getServentCount(); i++) {
			for (int j = 0; j < AppConfig.getServentCount(); j++) {
				if(i != j){
					if(AppConfig.getInfoById(i).getNeighbors().contains(j)
						&& AppConfig.getInfoById(j).getNeighbors().contains(i)){

						int ijAmount = 0;
						LiSnapshotResult ijNodeResult = getFromCollectedLiBySnapshotAndProccesId(snapshotVersion, i);
						if(ijNodeResult != null){
							ijAmount = ijNodeResult.getGiveHistory().get(j);
						}

						int jiAmount = 0;//Treba drugacije proveriti
						LiSnapshotResult jiNodeResult = getFromCollectedLiBySnapshotAndProccesId(snapshotVersion, j);
						if(jiNodeResult != null){
							jiAmount = jiNodeResult.getGetHistory().get(i);
						}

						if(ijAmount != jiAmount){
							String outputString = String.format(
									"Unreceived bitcake amount: %d from servent %d to servent %d",
									ijAmount - jiAmount, i, j);
							AppConfig.timestampedStandardPrint(outputString);
							sum += ijAmount - jiAmount;
						}
					}
				}
			}
		}

		AppConfig.timestampedStandardPrint("System bitcake count" + sum
				+ ", for snapshot version " + snapshotVersion.getSnapshot_no().get());
		collecting.set(false);
		}
	}

	private LiSnapshotResult getFromCollectedLiBySnapshotAndProccesId(SnapshotVersion snapshotVersion, int id){
		synchronized (AppConfig.LiLOCK){
			for(LiKey key : collectedLiValues.keySet()){
				if(key.getSnapshotVersion().get() == snapshotVersion.getSnapshot_no().get()
						&& key.getProcessId().get() == id){
					return collectedLiValues.get(key);
				}
			}
		}
		return null;
	}

	private void testPrintCollectedLi(){
		AppConfig.timestampedStandardPrint("Li Size: " + collectedLiValues.size());
		for(Entry<LiKey, LiSnapshotResult> entry : collectedLiValues.entrySet()){
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("init_id:")
					.append(entry.getKey().getProcessId().get())
					.append("snapshot_no:")
					.append(entry.getKey().getSnapshotVersion().get())
					.append("| recordedAmount:")
					.append(entry.getValue().getRecordedAmount())
					.append(",give:")
					.append(entry.getValue().getGiveHistory())
					.append(",get:")
					.append(entry.getValue().getGiveHistory())
					.append("\n");
			AppConfig.timestampedStandardPrint(stringBuilder.toString());
		}
	}

	private void printLiVersionTwo(SnapshotVersion snapshotVersion){
		synchronized (AppConfig.LiLOCK){
			int sum = 0;
			for(Entry<LiKey, LiSnapshotResult> entry : collectedLiValues.entrySet()){
				if(snapshotVersion.getSnapshot_no().get() == entry.getKey().getSnapshotVersion().get()){
					sum += entry.getValue().getRecordedAmount();
				}
			}
			for(Entry<LiKey, LiSnapshotResult> entry : collectedLiValues.entrySet()){
				if(snapshotVersion.getSnapshot_no().get() == entry.getKey().getSnapshotVersion().get()){
					for(Entry<Integer, Integer> entry1 : entry.getValue().getGiveHistory().entrySet()){
						sum += entry1.getValue();
					}
				}
			}
			for(Entry<LiKey, LiSnapshotResult> entry : collectedLiValues.entrySet()){
				if(snapshotVersion.getSnapshot_no().get() == entry.getKey().getSnapshotVersion().get()){
					for(Entry<Integer, Integer> entry1 : entry.getValue().getGetHistory().entrySet()){
						sum -= entry1.getValue();
					}
				}
			}

			AppConfig.timestampedStandardPrint("System bitcake count" + sum
					+ ", for snapshot version " + snapshotVersion.getSnapshot_no().get());
			collecting.set(false);
		}
	}


	private int summing(LiSnapshotResult liSnapshotResult){
		synchronized (AppConfig.LiLOCK){
			int sum = 0;
			sum += liSnapshotResult.getRecordedAmount();

			for (Entry<Integer, Integer> entry : liSnapshotResult.getGiveHistory().entrySet()){
				sum += entry.getValue();
			}

			for (Entry<Integer, Integer> entry : liSnapshotResult.getGetHistory().entrySet()){
				sum -= entry.getValue();
			}

			return sum;
		}
	}
}
