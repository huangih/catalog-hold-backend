package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingCloseDate implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2674440532406445247L;

	@Id
	private int id;

	private LocalDate begDate;

	private LocalDate endDate;

	private String siteCodes = "ALL";

	public BookingCloseDate(int id) {
		this.id = id;
	}

	public BookingCloseDate(LocalDate begDate, LocalDate endDate, String siteCode) {
		this.begDate = begDate;
		this.endDate = endDate;
		this.siteCodes = siteCode;
	}

}
