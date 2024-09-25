package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TransitOverdays implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3766153584098044948L;

	@Id
	private int id;
	
	private int holdId;

	private int fromSiteId;

	private int toSiteId;

	private int siteId;

	private int dueSiteId;

	private int weeks;

	private Phase phase;

	private LocalDateTime transitDate = LocalDateTime.now();

	private LocalDateTime relayDate;

	private LocalDateTime touchTime;
	
	private int muserId = 500;

	private LocalDateTime createDate = LocalDateTime.now();

	public TransitOverdays(int holdId, int fromSiteId, int toSiteId, Phase phase, int muserId) {
		this.holdId = holdId;
		this.fromSiteId = fromSiteId;
		this.toSiteId = toSiteId;
		this.phase = phase;
		this.muserId = muserId;
	}

	public TransitOverdays(Intransit intransit, int siteId) {
		BeanUtils.copyProperties(intransit, this);
		this.siteId = siteId;
		this.compDueSiteId(this.transitDate.toLocalDate());
	}

	private void compDueSiteId(LocalDate transitDate) {
		LocalDate today = LocalDate.now();
		if (transitDate.isBefore(today.minusDays(21))) {
			this.weeks = 3;
			this.dueSiteId = this.siteId;
		} else if (transitDate.isBefore(today.minusDays(14))) {
			this.weeks = 2;
			this.dueSiteId = this.toSiteId;
		} else if (transitDate.isBefore(today.minusDays(7))) {
			this.weeks = 1;
			this.dueSiteId = this.fromSiteId;
		}
	}

}
