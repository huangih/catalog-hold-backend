package tw.com.hyweb.cathold.model;

import java.io.Serializable;

import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Table("vbooking_distribution_num")
@NoArgsConstructor
public class VBookingDistributionNum implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4151881199394120562L;

	@Embedded.Empty
	private BookingDistriNumId bookingDistriNumId;
	
	private String siteCode;

	private int distriNum;

	private int days;

	private int dueDistri;
}
