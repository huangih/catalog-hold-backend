package tw.com.hyweb.cathold.model.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lombok.Data;

@Data
public class TouchControl {

	private int reqNum;

	private int lastCallback;

	private Map<Integer, String> preMap = new HashMap<>();

	private Lock lock = new ReentrantLock();

	private Condition condition = lock.newCondition();

	public TouchControl(int reqNum) {
		this.reqNum = reqNum;
	}

	public void preTouchCallback(PreTouchResult preTouchResult) {
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
			reqNum += n;
			this.condition.signal();
		} finally {
			lock.unlock();
		}
	}

	public Map<Integer, String> waitPostMap() {
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

}
