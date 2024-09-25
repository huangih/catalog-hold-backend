package tw.com.hyweb.cathold.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
public enum TransitReason {
	BOOKING("B"), RETURN("R"), NONE("");

	@Getter
	private String name;
}
