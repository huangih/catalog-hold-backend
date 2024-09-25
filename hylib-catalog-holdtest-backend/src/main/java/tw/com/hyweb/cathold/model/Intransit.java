package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(of = { "holdId" })
@NoArgsConstructor
public class Intransit implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3766153584098044948L;

	@Id
	private int holdId;

	private int fromSiteId;

	private int toSiteId;

	private Phase phase;

	private LocalDateTime transitDate = LocalDateTime.now();

	private LocalDateTime relayDate;

	private int muserId = 500;

	private LocalDateTime updateDate;

	private String barcode;

	public Intransit(int holdId, int fromSiteId, int toSiteId, Phase phase) {
		this.holdId = holdId;
		this.fromSiteId = fromSiteId;
		this.toSiteId = toSiteId;
		this.phase = phase;
	}

	public Intransit(int holdId, int fromSiteId, int toSiteId, Phase phase, LocalDateTime transitDate,
			String barcode) {
		this.holdId = holdId;
		this.fromSiteId = fromSiteId;
		this.toSiteId = toSiteId;
		this.phase = phase;
		if (transitDate != null)
			this.transitDate = transitDate;
		this.barcode = barcode;
	}

	public Intransit(VIntransitBooking vIntransitBooking) {
		BeanUtils.copyProperties(vIntransitBooking, this);
	}

}
