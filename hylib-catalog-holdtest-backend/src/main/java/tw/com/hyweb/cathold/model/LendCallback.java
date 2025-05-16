package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.util.ArrayList;
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

	private int readerId;

	private int holdId;

	private int muserId;

	private List<Character> callbackTypes = new ArrayList<>();

	private char canotLendType;

	private String reason;

	private int logId;

	@JsonIgnore
	private boolean timeout;

	public LendCallback(LendLog2 lendLog2) {
		this.logId = lendLog2.getId();
		this.readerId = lendLog2.getReaderId();
		this.holdId = lendLog2.getHoldId();
		this.muserId = lendLog2.getMuserId();
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

	public void addCallbackType(char type) {
		this.callbackTypes.add(type);
	}

	public Mono<LendCallback> timeout(String timeoutError, char type) {
		this.timeout = true;
		this.canotLendType = type;
		this.reason = "LendCallback-Timeout";
		log.error("LendCallback-Timeout:{}-{}", timeoutError, this);
		return Mono.just(this);
	}

}
