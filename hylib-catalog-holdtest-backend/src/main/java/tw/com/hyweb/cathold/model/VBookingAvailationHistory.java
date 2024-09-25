package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Table("vbooking_availation_history")
public class VBookingAvailationHistory implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5600854864811122906L;

	@Id
	private long bookingId;

	private int userId;

	private int pickupSiteId;

	private String siteCode;

	private int holdId;

	private int statusId;

	private int seqNum;

	private int type;

	private int noticeId;

	private LocalDate availableDate;

	private LocalDate duePickupDate;

	private Phase phase;

}
