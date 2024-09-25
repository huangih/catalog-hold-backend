package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ItemSiteDef implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6604767261631105123L;

	@Id
	private int siteId;

	private String siteCode;

	private String siteName;

	private int dueDistri;

	private boolean pickupSite;

	private Integer pickupRule;

	private LocalDate pickupDate;

	private String clyNum;

	private int pickupDueDays;

	private boolean expandAvail;

	private Integer expandRule;

	private LocalDate availDate;

	private int pickupLimit;

	private int distriSite;

	public ItemSiteDef(short siteId) {
		this.siteId = siteId;
	}

	public ItemSiteDef(String code) {
		this.siteCode = code;
	}

	public boolean canPickup() {
		return this.pickupSite ^ LocalDate.now().isBefore(this.pickupDate);
	}

	public boolean canExpand(LocalDateTime availDate) {
		return this.expandAvail ^ availDate.toLocalDate().isBefore(this.availDate);
	}
}