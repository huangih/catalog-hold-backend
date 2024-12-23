package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.BeanUtils;

import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.Booking;
import tw.com.hyweb.cathold.model.Phase;

@NoArgsConstructor
@Data
public class BookingNclView implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1572431173774809086L;

	private long id;

	private int userId;

	private LocalDateTime placeDate;

	private MarcVolume marcVolume;

	private Phase phase;

	private LocalDate dueDate;

	private LocalDateTime updateTime;

	public BookingNclView(Booking booking) {
		BeanUtils.copyProperties(booking, this);
	}

}
