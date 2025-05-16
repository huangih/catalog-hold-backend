package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.time.LocalDate;
import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.AppendixStatus;
import tw.com.hyweb.cathold.model.McvBookingStatus;

@Data
@NoArgsConstructor
public class BookingStatusView implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3447020971137508994L;

	private int marcId;

	private String callVolume;

	private AppendixStatus appendixStatus;

	private int holdId;

	private int supportNum;

	private int requestNum;

	private int rate;

	private int waitDays;

	@JsonSerialize(using = LocalDateSerializer.class)
	@JsonDeserialize(using = LocalDateDeserializer.class)
	private LocalDate updateDate;

	public BookingStatusView(McvBookingStatus mcvBookingStatus) {
		BeanUtils.copyProperties(mcvBookingStatus, this);
	}

	public void setMarcVolume(MarcVolume marcVolume) {
		this.marcId = marcVolume.getMarcId();
		this.callVolume = marcVolume.getCallVolume();
		this.appendixStatus = marcVolume.getAppendixStatus();
	}

}
