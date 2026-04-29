package tw.com.hyweb.cathold.model;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import tw.com.hyweb.cathold.model.client.VHoldClient;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TouchObj {

	private static final List<Character> TOUCH_PRECHARS = List.of('-', '/', '!');

	@Include
	private int touchLogId;

	private TouchLog touchLog;

	private VHoldClient vHoldClient;

	private VHoldItem vHoldItem;

	private char floatGroup;

	private char itemGroup;

	private Phase transitPhase;

	private char ctrlChar;

	private String barcode;

	private String sessionId;

	private int muserId;

	public TouchObj(TouchLog touchLog, VHoldClient vhc, String barcode, String sessionId, int muserId) {
		this.touchLogId = touchLog.getId();
		this.touchLog = touchLog;
		this.vHoldClient = vhc;
		this.floatGroup = vhc.getFloatGroup();
		this.barcode = barcode;
		char c = barcode.charAt(0);
		if (TOUCH_PRECHARS.contains(c)) {
			this.ctrlChar = c;
			this.barcode = barcode.substring(1);
		}
		this.muserId = muserId;
		this.sessionId = sessionId;
	}

	public void setVHoldItem(VHoldItem vHoldItem) {
		this.vHoldItem = vHoldItem;
		String itemG = this.vHoldItem.getFloatGroup();
		if (itemG != null)
			this.itemGroup = itemG.charAt(0);
	}

	public boolean compFloatTouch() {
		return this.floatGroup > 0 && this.floatGroup == this.itemGroup && vHoldItem.floatItem()
				&& vHoldClient.isFloatReceive();
	}

	public boolean isFloatPriority(char group) { // 圖書群組為(A-Z)於client群組(A-Z)點收傳回真,將不作預約調撥予預約者
		return itemGroup > 0 && itemGroup < group && this.floatGroup > 0 && this.floatGroup < group;
	}

}
