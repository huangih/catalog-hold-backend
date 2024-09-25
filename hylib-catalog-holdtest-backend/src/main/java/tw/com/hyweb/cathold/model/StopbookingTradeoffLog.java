package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StopbookingTradeoffLog implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1570151221827246695L;

	@Id
	private int id;

	private int userId;

	private int tradeoffDays;

	private LocalDate oldDate;

	private LocalDate newDate;

	private int muserId;

	private int correlationId;

	private LocalDateTime createTime = LocalDateTime.now();

	public StopbookingTradeoffLog(UserStopBooking usb, int days, LocalDate endDate) {
		this.correlationId = usb.getId();
		this.tradeoffDays = days;
		this.oldDate = endDate;
		this.userId = usb.getUserId();
		this.newDate = usb.getEndDate();
	}

}
