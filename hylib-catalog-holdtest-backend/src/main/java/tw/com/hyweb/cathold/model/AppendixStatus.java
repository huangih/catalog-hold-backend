package tw.com.hyweb.cathold.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
public enum AppendixStatus {
	NONE("N"), EXIST("E"), MISS("M"), DELCARE("T"), DOUBLEVIEW("DV"), PERIODMERGE("PM"), ANNEX_NOMAIN("AN");

	@Getter
	private String status;
}
