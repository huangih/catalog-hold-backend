package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.Intransit;
import tw.com.hyweb.cathold.model.Phase;
import tw.com.hyweb.cathold.model.TransitReason;

@Data
@NoArgsConstructor
public class IntransitView implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 376270263042635921L;

	private int holdId;

	private int fromSiteId;

	private int toSiteId;

	private TransitReason reason;

	private OffsetDateTime transitTime;

	private OffsetDateTime relayTime;

	public IntransitView(Intransit intransit) {
		this.holdId = intransit.getHoldId();
		this.fromSiteId = intransit.getFromSiteId();
		this.toSiteId = intransit.getToSiteId();
		Phase phase = intransit.getPhase();
		this.reason = (phase == Phase.TRANSIT_R || phase == Phase.WAIT_TRANSITR) ? TransitReason.RETURN
				: TransitReason.BOOKING;
		this.transitTime = intransit.getTransitDate().atOffset(ZoneOffset.UTC);
		if (intransit.getRelayDate() != null)
			this.relayTime = intransit.getRelayDate().atOffset(ZoneOffset.UTC);
	}
}
