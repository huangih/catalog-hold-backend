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
@Table("vbooking_avail_remove")
public class VBookingAvailRemove implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5170921424564795152L;

	@Id
	private long bookingId;

	private int readerId;

	private int siteId;

	private String siteCode;

	private int holdId;

	private int seqNum;

	private int type;

	private String mark;

	private LocalDateTime availableDate;

	private LocalDate duePickupDate;

	private Phase phase;

	private LocalDateTime removeDate;

}
