package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.SuspendPhase;
import tw.com.hyweb.cathold.model.UserSuspendBooking;

@Data
@NoArgsConstructor
public class UserSuspendBookingView implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4538457578532472449L;

	private int id;

	private int userId;

	@JsonSerialize(using = LocalDateSerializer.class)
	@JsonDeserialize(using = LocalDateDeserializer.class)
	private LocalDate begDate;

	@JsonSerialize(using = LocalDateSerializer.class)
	@JsonDeserialize(using = LocalDateDeserializer.class)
	private LocalDate endDate;

	private SuspendPhase phase = SuspendPhase.SUSPEND_PLACE;

	private int muserId;

	private OffsetDateTime createTime;

	private OffsetDateTime updateTime;

	public UserSuspendBookingView(UserSuspendBooking usb) {
		BeanUtils.copyProperties(usb, this);
		this.createTime = usb.getCreateTime().atOffset(ZoneOffset.UTC);
		this.updateTime = usb.getUpdateTime().atOffset(ZoneOffset.UTC);
	}

}
