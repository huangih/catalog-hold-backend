package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.UserStopBooking;

@Data
@NoArgsConstructor
public class ReaderStopBookingInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1008293474331769624L;

	private int userId;

	private LocalDate begDate;

	private LocalDate endDate;

	private List<BookingHistoryView> bookingHistoryViews;

	private OffsetDateTime createTime;

	private OffsetDateTime updateTime;

	public ReaderStopBookingInfo(UserStopBooking userStopBooking) {
		this.userId = userStopBooking.getUserId();
		this.begDate = userStopBooking.getBegDate();
		this.endDate = userStopBooking.getEndDate();
		this.createTime = userStopBooking.getCreateTime().atOffset(ZoneOffset.UTC);
		this.updateTime = userStopBooking.getUpdateTime().atOffset(ZoneOffset.UTC);
	}

}
