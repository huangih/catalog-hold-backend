package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.BookingHistory;
import tw.com.hyweb.cathold.model.Phase;

@Data
@NoArgsConstructor
public class BookingHistoryView implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5399572885497871066L;

	private long bookingId;

	private int userId;

	private int holdId;

	private MarcVolume marcVolume;

	private OffsetDateTime placeDate;

	private int pickupSiteId;

	private String type;

	private String comment;

	private Phase phase;

	private OffsetDateTime availableDate;

	private LocalDate dueDate;

	private int noticeId;

	private boolean canRollBack;

	private OffsetDateTime inActiveDate;

	private OffsetDateTime updateTime;

	public BookingHistoryView(BookingHistory bookingHistory) {
		this.bookingId = bookingHistory.getId();
		this.userId = bookingHistory.getUserId();
		this.pickupSiteId = bookingHistory.getPickupSiteId();
		this.type = String.valueOf(bookingHistory.getType());
		this.placeDate = bookingHistory.getPlaceDate().atOffset(ZoneOffset.UTC);
		this.phase = bookingHistory.getPhase();
		if (bookingHistory.getAvailableDate() != null)
			this.availableDate = bookingHistory.getAvailableDate().atOffset(ZoneOffset.UTC);
		if (bookingHistory.getDueDate() != null)
			this.dueDate = bookingHistory.getDueDate();
		if (bookingHistory.getInActiveDate() != null)
			this.inActiveDate = bookingHistory.getInActiveDate().atOffset(ZoneOffset.UTC);
		this.updateTime = bookingHistory.getUpdateTime().atOffset(ZoneOffset.UTC);
		this.canRollBack = this.phase == Phase.FILLED
				&& bookingHistory.getInActiveDate().toLocalDate().isAfter(LocalDate.now().minusDays(8));
	}
}
