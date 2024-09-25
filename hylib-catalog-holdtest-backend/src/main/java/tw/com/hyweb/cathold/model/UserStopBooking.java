package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserStopBooking implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2322495728708690468L;

	@Id
	private int id;

	private int userId;

	private LocalDate begDate;

	private LocalDate endDate;

	private LocalDate oriEndDate;

	private boolean available = true;

	private LocalDateTime createTime;

	private LocalDateTime updateTime;

	public UserStopBooking(int userId) {
		this.userId = userId;
	}

	public int tradeoffDays(int tradeoffDays) {
		if (this.oriEndDate == null)
			this.oriEndDate = this.endDate;
		int maxTradeoffDays = ((int) LocalDate.now().until(this.endDate, ChronoUnit.DAYS)) + 1;
		if (tradeoffDays > maxTradeoffDays)
			tradeoffDays = maxTradeoffDays;
		this.endDate = this.endDate.minusDays(tradeoffDays);
		if (this.endDate.isBefore(LocalDate.now()))
			this.available = false;
		return tradeoffDays;
	}

	public int rollbackTradeoffDays(int rDays) {
		int maxRdays = (int) this.oriEndDate.until(this.endDate, ChronoUnit.DAYS);
		if (rDays < maxRdays)
			rDays = maxRdays;
		this.endDate = this.endDate.minusDays(rDays);
		if (!this.endDate.isBefore(LocalDate.now()))
			this.available = true;
		return rDays;
	}
}
