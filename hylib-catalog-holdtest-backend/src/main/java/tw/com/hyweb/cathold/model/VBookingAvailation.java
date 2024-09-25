package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Table("vbooking_availation")
public class VBookingAvailation implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7565680598225837588L;

	@Id
	private long bookingId;

	private int userId;

	private int pickupSiteId;

	private String siteCode;

	private int holdId;

	private int seqNum;

	private int type;

	private String mark;

	private int noticeId;

	private Character ntype;

	private Boolean linepush;

	private LocalDateTime availableDate;

	private LocalDate duePickupDate;

	private Phase phase;

}
