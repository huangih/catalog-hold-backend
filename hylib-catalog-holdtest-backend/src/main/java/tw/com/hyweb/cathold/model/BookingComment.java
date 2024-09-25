package tw.com.hyweb.cathold.model;

import java.io.Serializable;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BookingComment implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1673698206457699980L;

	@Id
	private long id;

	private String comment;

	public BookingComment(long bookingId) {
		this.id = bookingId;
	}

}
