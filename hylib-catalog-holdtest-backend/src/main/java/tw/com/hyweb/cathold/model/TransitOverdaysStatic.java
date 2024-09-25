package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TransitOverdaysStatic implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5041520014437339992L;

	@Id
	private int id;

	private String siteCode;

	private LocalDate createDate;

	private int needFind1;

	private int notFind1;

	private int needFind2;

	private int notFind2;

	private int needFind3;

	private int notFind3;

	private int notMissing;
	
	public TransitOverdaysStatic(String siteCode, LocalDate createDate) {
		this.siteCode = siteCode;
		this.createDate = createDate;
	}

}
