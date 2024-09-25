package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BookingExpandDuedate implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8009630613887366862L;

	@Id
	private long bookingId;

	private int readerId;

	private LocalDate oriDueDate;

	private LocalDate expDueDate;

	private int muserId;

	private LocalDateTime createTime = LocalDateTime.now();

	public BookingExpandDuedate(Booking booking, int muserId) {
		this.bookingId = booking.getId();
		this.readerId = booking.getUserId();
		this.oriDueDate = booking.getDueDate();
		this.muserId = muserId;
	}

}
