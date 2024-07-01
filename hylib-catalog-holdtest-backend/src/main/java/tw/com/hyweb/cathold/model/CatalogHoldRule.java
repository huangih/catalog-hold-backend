package tw.com.hyweb.cathold.model;

import java.io.Serializable;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CatalogHoldRule implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 988405087903837190L;

	@Id
	private int id;

	private String ruleClassName;

	protected String ruleExp;

	protected String ruleArgs;

	private int ruleSequence = 0;

	public CatalogHoldRule(String ruleClassName) {
		this.ruleClassName = ruleClassName;
	}
	
}
