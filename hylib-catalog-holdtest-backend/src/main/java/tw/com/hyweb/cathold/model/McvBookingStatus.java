package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class McvBookingStatus implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -590565056584924545L;

	@Id
	private int callVolId;

	private int holdId;

	private int supportNum;

	private int requestNum;

	private int rate;

	private int waitDays;

	private LocalDate updateDate;

	public McvBookingStatus(int id) {
		this.callVolId = id;
	}

}
