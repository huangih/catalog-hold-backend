package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.time.LocalDate;
import org.springframework.beans.BeanUtils;

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
