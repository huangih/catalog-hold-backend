package tw.com.hyweb.cathold.model.client;

import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PreTouchResult implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5465317967681254786L;

	private int priority;

	private boolean postProcess;

	private String paramId;

	private String status;

	public PreTouchResult(int i) {
		this.priority = 1 << i;
	}

}
