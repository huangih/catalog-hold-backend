package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class VHotBookingDate implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5454528721856277424L;

	@Id
	private int id;

	@EqualsAndHashCode.Include
	private int callVolId;

	private LocalDate statusDate;

	private boolean hotBooking;

	private boolean hotStatus;

	private int rate;

	private int supNum;

	private int reqNum;

	public VHotBookingDate(int callVolId) {
		this.callVolId = callVolId;
	}

}
