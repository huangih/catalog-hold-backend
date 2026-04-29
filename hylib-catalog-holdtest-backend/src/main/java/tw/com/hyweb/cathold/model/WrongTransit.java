package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class WrongTransit implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3855188339398066404L;

	private int id;

	private int holdId;

	private String barcode;

	private String fromSiteCode;

	private String toSiteCode;

	private String reachSiteCode;

	private Phase phase;

	private LocalDateTime transitDate;

	private LocalDateTime relayDate;

	private LocalDateTime reachDate;

	private int muserId;

	private boolean diffGroup;

}
