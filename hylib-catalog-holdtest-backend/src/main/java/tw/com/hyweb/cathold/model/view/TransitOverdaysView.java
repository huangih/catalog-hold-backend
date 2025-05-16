package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.Phase;
import tw.com.hyweb.cathold.model.TransitOverdays;
import tw.com.hyweb.cathold.model.TransitReason;

@Data
@NoArgsConstructor
public class TransitOverdaysView implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3766153584098044948L;

	private int holdId;

	private String fromSiteCode;

	private String toSiteCode;

	private String siteCode;

	private String dueSiteCode;

	private int weeks;

	private TransitReason reason;

	private LocalDateTime transitDate;

	private LocalDateTime relayDate;

	private int muserId = 500;

	private LocalDateTime createDate;

	public TransitOverdaysView(TransitOverdays transitOverdays) {
		this.holdId = transitOverdays.getHoldId();
		this.weeks = transitOverdays.getWeeks();
		this.muserId = transitOverdays.getMuserId();
		Phase phase = transitOverdays.getPhase();
		this.reason = (phase == Phase.TRANSIT_R || phase == Phase.WAIT_TRANSITR) ? TransitReason.RETURN
				: TransitReason.BOOKING;
		this.createDate = transitOverdays.getCreateDate();
		this.transitDate = transitOverdays.getTransitDate();
		if (transitOverdays.getRelayDate() != null)
			this.relayDate = transitOverdays.getRelayDate();
	}

}
