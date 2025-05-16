package tw.com.hyweb.cathold.model.view;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.BeanUtils;

import lombok.Data;
import tw.com.hyweb.cathold.model.BookingExpandDuedate;

@Data
public class ExpandDuedateView {

	private LocalDate oriDueDate;

	private LocalDate expDueDate;

	private OffsetDateTime expandTime;

	private BookingHistoryView bookingHistoryView;

	public ExpandDuedateView(BookingExpandDuedate bookingExpandDuedate) {
		BeanUtils.copyProperties(bookingExpandDuedate, this);
		this.expandTime = bookingExpandDuedate.getCreateTime().atOffset(ZoneOffset.UTC);
	}
}
