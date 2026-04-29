package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BookingCabinet implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5017822559156759681L;

	@Id
	private long bookingId;

//	private int siteId;
//
	private int cabinetId;

	private LocalDateTime updateTime = LocalDateTime.now();

	public BookingCabinet(Booking booking, CabinetDef cabinetDef) {
		this.bookingId = booking.getId();
//		this.siteId = cabinetDef.getSiteId();
		this.cabinetId = cabinetDef.getId();
	}

}
