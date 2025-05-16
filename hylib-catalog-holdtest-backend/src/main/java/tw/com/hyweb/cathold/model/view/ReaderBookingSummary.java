package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.util.function.Tuple5;
import tw.com.hyweb.cathold.model.Phase;
import tw.com.hyweb.cathold.model.UserSuspendBooking;

@Data
@NoArgsConstructor
public class ReaderBookingSummary implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5630147828413060055L;

	private int userId;

	private int bookingNum;

	private int availBookingNum;

	private LocalDate stopBegDate;

	private LocalDate stopEndDate;

	private List<BookingView> availBookingViews;

	private List<BookingHistoryView> overDueBookings;

	private UserSuspendBooking userSuspendBooking;

	private List<String> availSeqNums;

	private int availOverDueNum;

	private int onStopOverDueNum;

	private int bookingExpandDueOnMonth;

//	public ReaderBookingSummary(int userId, int bookingNum, List<BookingView> availBookingViews,
//			List<BookingHistoryView> overDueBookings, List<BookingExpandDuedate> beds, UserSuspendBooking usb) {
//		this.userId = userId;
//		this.bookingNum = bookingNum;
//		this.availBookingViews = availBookingViews;
//		this.availBookingNum = availBookingViews.size();
//		this.overDueBookings = overDueBookings;
//		this.onStopOverDueNum = (int) overDueBookings.stream()
//				.filter(o -> o.getPhase() == Phase.ON_STOP_BOOKING).count();
//		this.availOverDueNum = overDueBookings.size() - this.onStopOverDueNum;
//		this.availSeqNums = this.availBookingViews.stream().map(BookingView::getAvailSeqNum).toList();
//		this.bookingExpandDueOnMonth = beds == null || beds.isEmpty() ? 0 : beds.size();
//		this.userSuspendBooking = usb;
//	}
//
	public ReaderBookingSummary(
			Tuple5<Integer, List<BookingView>, List<BookingHistoryView>, Integer, UserSuspendBooking> tup5) {
		this.bookingNum = tup5.getT1();
		this.availBookingViews = tup5.getT2();
		this.availBookingNum = this.availBookingViews.size();
		this.overDueBookings = tup5.getT3();
		this.onStopOverDueNum = (int) this.overDueBookings.stream().filter(o -> o.getPhase() == Phase.ON_STOP_BOOKING)
				.count();
		this.availOverDueNum = this.overDueBookings.size() - this.onStopOverDueNum;
		this.availSeqNums = this.availBookingViews.stream().map(BookingView::getAvailSeqNum).toList();
		this.bookingExpandDueOnMonth = tup5.getT4();
		this.userSuspendBooking = tup5.getT5();
	}

}
