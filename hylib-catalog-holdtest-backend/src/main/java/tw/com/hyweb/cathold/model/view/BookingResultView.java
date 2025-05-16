package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BookingResultView implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3814750828635282004L;

	private long id;

	private String resultCode;

	private BookingView booking;

	private BookingHistoryView bookingHistory;

	public BookingResultView(long id, String resultCode) {
		this.id = id;
		this.resultCode = resultCode;
	}

}
