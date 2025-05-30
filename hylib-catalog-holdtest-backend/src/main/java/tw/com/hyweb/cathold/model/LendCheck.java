package tw.com.hyweb.cathold.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LendCheck implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 761268134515009290L;

	private boolean canLend = true;

	private String reason;

	private String callbackId;

	private char type = '_';

	public LendCheck(char type) {
		this.type = type;
	}

	public LendCheck(boolean canLend, String callbackId, char type) {
		this.canLend = canLend;
		this.callbackId = callbackId;
		this.type = type;
	}

	public LendCheck(String callbackId, char type) {
		this.callbackId = callbackId;
		this.canLend = false;
		this.reason = callbackId + "-Timeout";
		this.type = type;
	}

	public LendCheck(String callbackId, LendCallback lendCallback) {
		if (callbackId != null)
			this.callbackId = callbackId;
		this.canLend = lendCallback.getCanotLendType() == 0;
		if (!this.canLend) {
			this.reason = lendCallback.getReason();
			this.type = lendCallback.getCanotLendType();
		}
	}

}
