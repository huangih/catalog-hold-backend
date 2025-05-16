package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.Phase;
import tw.com.hyweb.cathold.model.VBookingAvailRemove;
import tw.com.hyweb.cathold.model.VBookingAvailation;

@Data
@NoArgsConstructor
public class BookingAvailationView implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3222401492125839740L;

	private long bookingId;

	private int readerId;

	private int holdId;

	private int pickupSiteId;

	private String availSeqNum;

	private Phase phase;

	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime availableDate;

	@JsonSerialize(using = LocalDateSerializer.class)
	@JsonDeserialize(using = LocalDateDeserializer.class)
	private LocalDate duePickupDate;

	public BookingAvailationView(VBookingAvailation vBookingAvailation) {
		BeanUtils.copyProperties(vBookingAvailation, this);
		this.readerId = vBookingAvailation.getUserId();
		this.availSeqNum = String.format("%03d", vBookingAvailation.getSeqNum());
		String s = vBookingAvailation.getMark();
		if (s != null && !s.isEmpty())
			this.availSeqNum += "(" + vBookingAvailation.getMark() + ")";
		if (vBookingAvailation.getType() > 0)
			this.availSeqNum += "#" + vBookingAvailation.getType();
	}

	public BookingAvailationView(VBookingAvailRemove vBookingAvailRemove) {
		BeanUtils.copyProperties(vBookingAvailRemove, this);
		this.pickupSiteId = vBookingAvailRemove.getSiteId();
		this.availSeqNum = String.format("%03d", vBookingAvailRemove.getSeqNum());
		String s = vBookingAvailRemove.getMark();
		if (s != null && !s.isEmpty())
			this.availSeqNum += "(" + vBookingAvailRemove.getMark() + ")";
		if (vBookingAvailRemove.getType() > 0)
			this.availSeqNum += "#" + vBookingAvailRemove.getType();
	}

	public BookingAvailationView(int holdId) {
		this.holdId = holdId;
	}

}
