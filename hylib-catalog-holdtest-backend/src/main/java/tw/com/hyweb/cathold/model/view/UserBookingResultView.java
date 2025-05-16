package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserBookingResultView implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 878774846495445175L;

	private String resultCode;

	private UserSuspendBookingView readerSuspendBooking;

	public UserBookingResultView(String resultCode) {
		this.resultCode = resultCode;
	}

}
