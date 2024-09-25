package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class LendCallback implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8830788255622595384L;

	private static final List<Character> TYPES = Arrays.asList('F', 'M', 'B', 'T', 'O', 'U', 'D', '7', '9');

	@Transient
	@JsonIgnore
	private final Object checkLock = new Object();

	public static char compLastType(char lastType) {
		return TYPES.get(lastType - '1');
	}

	private int readerId;

	private int holdId;

	private String barcode;

	private String status;

	private int muserId;

	@Builder.Default
	private List<Character> callbackTypes = new ArrayList<>();

	@Builder.Default
	private int canotLendIndex = Integer.MAX_VALUE;

	private String reason;

	private LocalDateTime begTime;

	private int logId;

	private char lastType;

	@Synchronized("checkLock")
	public boolean lendCheck(LendCheck lendCheck) {
		String error = lendCheck.getCallbackId();
		if (error != null) {
			log.error("LendCallback-Timeout: {}-{}", error, this);
			return false;
		}
		this.lastType = lendCheck.getType();
		if ('@' < this.lastType)
			callbackTypes.add(this.lastType);
		if (!lendCheck.isCanLend()) {
			int index = TYPES.indexOf(this.lastType);
			if (index < this.canotLendIndex) {
				this.canotLendIndex = index;
				this.reason = lendCheck.getReason();
			}
		}
		return true;
	}

	@JsonIgnore
	public char getType() {
		if (this.canotLendIndex < Integer.MAX_VALUE)
			return TYPES.get(this.canotLendIndex);
		return '_';
	}

	@JsonIgnore
	public String getCallbackTypesString() {
		if (this.callbackTypes.isEmpty())
			return null;
		StringBuilder sb = new StringBuilder();
		for (Character type : this.callbackTypes)
			sb.append(type);
		return sb.toString();
	}

}
