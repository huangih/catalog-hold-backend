package tw.com.hyweb.cathold.model.view;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BookingHistories {

	private int count;
	
	private List<BookingHistoryView> bookingHistoryViews;
	
}
