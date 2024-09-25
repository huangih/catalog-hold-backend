package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.Booking;
import tw.com.hyweb.cathold.model.Phase;

@Data
@NoArgsConstructor
public class BookingView implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 788191258035485621L;

	private long bookingId;

	private int readerId;

	private int holdId;

	private MarcVolume marcVolume;

	private OffsetDateTime placeDate;

	private String type;

	private int position = 0;

	private IntransitView intransit;

	private String comment;

	private int pickupSiteId;

	private Phase phase;

	private int noticeId;

	private boolean canModify = false;

	private boolean canCanceled = false;
	
	private LocalDate distributeDate;
	
	private LocalDate availableDate;

	private LocalDateTime availableDateTime;

	private String availSeqNum;
	
	private boolean hotCallvol;

	private boolean hadExpDueDate;

	private boolean expDuedateSite = true;

	private boolean expDuedateType = true;
	
	private LocalDate duePickupDate;

	public BookingView(Booking booking) {
		this.bookingId = booking.getId();
		this.readerId = booking.getUserId();
		this.pickupSiteId = booking.getPickupSiteId();
		this.type = booking.getType();
		this.phase = booking.getPhase();
		this.placeDate = booking.getPlaceDate().atOffset(ZoneOffset.UTC);
	}
}
