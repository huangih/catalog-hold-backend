package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.Data;

@Data
public class TodayBookingInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5628983402789478744L;

	private LocalDate today = LocalDate.now();
	
	private int bookingUserNum;
	
	private int bookingNum;
}
