package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Booking implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1727115423478321579L;

	@Id
	private long id;

	private int userId;

	private int itemId;

	private int pickupSiteId;

	private String type = "T";

	private LocalDate expireDate = LocalDate.of(9999, 12, 31);

	private Phase phase = Phase.PLACE;

	private int associateId;

	private LocalDateTime placeDate = LocalDateTime.now();

	private LocalDateTime distributeDate;

	private LocalDateTime transitDate;

	private LocalDateTime availableDate;

	private LocalDate dueDate;

	private int muserId;

	private LocalDateTime createTime = LocalDateTime.now();

	private LocalDateTime updateTime = LocalDateTime.now();

	private int oldId;

	private long userKey;

	private String barcode;

	public Booking(int userId, int itemId, String type) {
		this.userId = userId;
		this.itemId = itemId;
		this.type = type;
	}

	public Booking(int userId, int itemId, String type, int pickupSiteId) {
		this(userId, itemId, type);
		this.pickupSiteId = pickupSiteId;
	}

	public Booking(int userId, int itemId, String type, int oldId, long userKey, String barcode) {
		this(userId, itemId, type);
		this.oldId = oldId;
		this.userKey = userKey;
		this.barcode = barcode;
	}

	public Booking transitSuspend() {
		Booking booking = new Booking(this.userId, this.itemId, this.type, this.pickupSiteId);
		booking.setPlaceDate(this.placeDate);
		booking.setPhase(Phase.SUSPENSION);
		return booking;
	}

	public Booking replaceBookingWithLend(int muserId, int callVolId) {
		int cvId = (this.type.equals("T") && callVolId > 0) ? callVolId : this.itemId;
		Booking booking = new Booking(this.userId, cvId, this.type, this.pickupSiteId);
		booking.setMuserId(muserId);
		booking.setPlaceDate(this.placeDate);
		return booking;
	}

	public Booking(BookingHistory bh) {
		BeanUtils.copyProperties(bh, this);
		this.phase = Phase.PLACE;
		this.associateId = 0;
		this.dueDate = null;
	}

	public Booking(Booking booking) {
		BeanUtils.copyProperties(booking, this);
		this.id = 0;
		this.distributeDate = null;
	}

}
