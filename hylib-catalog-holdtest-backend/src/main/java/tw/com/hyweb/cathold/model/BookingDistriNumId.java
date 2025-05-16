package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDistriNumId implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1576858340742414160L;

	private int dueSite;

	private LocalDate dueDate;

	private Phase phase;

}
