package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.time.LocalDate;

import org.springframework.beans.BeanUtils;

import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.TradeoffStopBookingResult;

@Data
@NoArgsConstructor
public class TradeoffStopBookingResultView implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 26927850020520866L;

	private String resultCode = "";

	private int userId;

	private LocalDate oriEndDate;

	private LocalDate newEndDate;

	private int reqTradeoffDays;

	private int tradeoffDays;

	private ReaderStopBookingInfo readerStopBookingInfo;

	public TradeoffStopBookingResultView(TradeoffStopBookingResult tradeoffStopBookingResult, int reqTradeoffDays,
			ReaderStopBookingInfo rsbi) {
		BeanUtils.copyProperties(tradeoffStopBookingResult, this);
		if (rsbi.getUserId() > 0)
			this.readerStopBookingInfo = rsbi;
		this.reqTradeoffDays = reqTradeoffDays;
	}

}
