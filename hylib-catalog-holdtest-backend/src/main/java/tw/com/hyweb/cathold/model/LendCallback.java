package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class LendCallback implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8830788255622595384L;

	private static final List<Character> TYPES = Arrays.asList('F', 'M', 'B', 'T', 'O', 'U', 'D', '7', '9');

	public static char compLastType(char lastType) {
		return TYPES.get(lastType - '0');
	}

	private int readerId;

	private int holdId;

	private int muserId;

	private List<Character> callbackTypes = new ArrayList<>();

	private int canotLendIndex = Integer.MAX_VALUE;

	private String reason;

	private int logId;

	private char lastType;

	public LendCallback(LendLog2 lendLog2) {
		this.logId = lendLog2.getId();
		this.readerId = lendLog2.getReaderId();
		this.holdId = lendLog2.getHoldId();
		this.muserId = lendLog2.getMuserId();
	}

	public Mono<Boolean> lendCheck(LendCheck lendCheck) {
		if (lendCheck.getCallbackId() != null)
			log.error("LendCallback-Timeout: {}-{}", lendCheck.getCallbackId(), this);
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
		return Mono.just(true);
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
