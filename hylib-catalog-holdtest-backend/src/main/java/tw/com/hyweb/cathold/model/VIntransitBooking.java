package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Table("vintransit_booking")
@NoArgsConstructor
public class VIntransitBooking implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3766153584098044948L;

	@Id
	private int holdId;

	private int fromSiteId;

	private int toSiteId;

	private String siteCode;

	private String clyMark;

	@Transient
	private String clyNum = "";

	private Phase phase;

	private LocalDateTime transitDate;

	private LocalDateTime relayDate;

	private LocalDateTime updateDate;

	private String barcode;

	private Long bookingId;

	private int callVolId;

	private Integer userId;

	private Integer pickupSiteId;

}
