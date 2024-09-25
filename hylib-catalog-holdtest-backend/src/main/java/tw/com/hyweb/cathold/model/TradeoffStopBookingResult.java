package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TradeoffStopBookingResult implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4618265953632910704L;

	private int userId;

	private ResultPhase resultPhase;

	private UserStopBooking userStopBooking;

	private LocalDate oriEndDate;

	private LocalDate newEndDate;

	private int tradeoffDays;

	public TradeoffStopBookingResult(ResultPhase resultPhase, StopbookingTradeoffLog stl,
			UserStopBooking userStopBooking) {
		this.resultPhase = resultPhase;
		this.userStopBooking = userStopBooking;
		this.userId = stl.getUserId();
		this.oriEndDate = stl.getOldDate();
		this.newEndDate = stl.getNewDate();
		this.tradeoffDays = stl.getTradeoffDays();
	}

	public TradeoffStopBookingResult(ResultPhase resultPhase, int userId) {
		this.resultPhase = resultPhase;
		this.userId = userId;
	}

}
