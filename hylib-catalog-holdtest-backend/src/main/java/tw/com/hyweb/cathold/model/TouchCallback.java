package tw.com.hyweb.cathold.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TouchCallback {

	private int type;

	private String funcName;

	private String routeKey;

	private Object[] args;

	public TouchCallback(int type) {
		this.type = type;
	}

}
