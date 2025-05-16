package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BookingDueDate implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6138827757042810948L;

	@Id
	private int id;

	private LocalDate begDuedate;

	private LocalDate endDuedate;

	private LocalDate dueDate;

	private String siteCode = "ALL";

	private boolean forExpand;

	private boolean overJustify;

	private LocalDateTime createTime = LocalDateTime.now();

	private LocalDateTime updateTime;

	public BookingDueDate(LocalDate begDate, LocalDate endDate, LocalDate dueDate, String siteCode, boolean expand,
			boolean overJustify) {
		this.begDuedate = begDate;
		this.endDuedate = endDate;
		this.dueDate = dueDate;
		this.siteCode = siteCode;
		this.forExpand = expand;
		this.overJustify = overJustify;
	}

}
