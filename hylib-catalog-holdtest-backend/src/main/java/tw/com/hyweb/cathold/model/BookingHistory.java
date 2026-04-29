package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BookingHistory implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3019149559629696837L;

	@Id
	private long id;

	private int userId;

	private int itemId;

	private int pickupSiteId;

	private String type = "T";

	private LocalDate expireDate;

	private Phase phase = Phase.NONE;

	private int associateId;

	private LocalDateTime placeDate;

	private LocalDateTime distributeDate;

	private LocalDateTime transitDate;

	private LocalDateTime availableDate;

	private LocalDate dueDate;

	private int pmuserId;

	private int muserId;

	private int oldId;

	private LocalDateTime inActiveDate;

	private LocalDateTime updateTime;

	public BookingHistory(Booking booking) {
		BeanUtils.copyProperties(booking, this);
	}

}
