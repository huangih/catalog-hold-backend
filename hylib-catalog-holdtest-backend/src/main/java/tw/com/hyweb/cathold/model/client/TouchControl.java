package tw.com.hyweb.cathold.model.client;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TouchControl {

	private String touchControlId;

	private int reqNum;

	private int lastCallback;

	private Map<Integer, String> preMap = new TreeMap<>();

	private Lock lock = new ReentrantLock();

	private Condition condition = lock.newCondition();

	public TouchControl(int reqNum, String tcId) {
		this.reqNum = reqNum;
		this.touchControlId = tcId;
	}

	public int preTouchCallback(PreTouchResult preTouchResult) {
		int n = preTouchResult.getPriority();
		this.lock.lock();
		try {
			this.lastCallback = n;
			if (preTouchResult.isPostProcess()) {
				preMap.put(n, preTouchResult.getParamId());
				n <<= 1;
			}
			if (preTouchResult.getStatus() != null)
				preMap.put(-1, preTouchResult.getStatus());
			this.reqNum += n;
			this.condition.signal();
		} finally {
			this.lock.unlock();
		}
		return this.reqNum;
	}

	public CompletableFuture<TouchControl> waitPreReady() {
		return CompletableFuture.supplyAsync(() -> {
			this.lock.lock();
			try {
				while (this.reqNum < 0) {
					this.condition.await();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				this.lock.unlock();
			}
			return this;
		});
	}

	public Map<Integer, String> waitPostMap() {
		this.lock.lock();
		try {
			while (this.reqNum < 0) {
				this.condition.await();
				log.info("tc: {}", this);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			this.lock.unlock();
		}
		return this.preMap;
	}

	public Map<Integer, String> waitPostMapTimeout() {
		this.lock.lock();
		try {
			this.preMap.put(1 << 17, String.valueOf(this.reqNum));
			this.reqNum = 0;
			this.condition.signal();
		} finally {
			lock.unlock();
		}
		return this.preMap;
	}

	public TouchControl waitPostMapTimeoutN() {
		this.lock.lock();
		try {
			this.preMap.put(1 << 17, String.valueOf(this.reqNum));
			this.reqNum = 0;
			this.condition.signal();
		} finally {
			lock.unlock();
		}
		log.info("waitPostMapTimeoutN: {}", this);
		return this;
	}

}
