package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Table("vbooking_cabinet_hold")
public class VBookingCabinetHold implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2083268473115121232L;

	@Id
	private long bookingId;

	private int cabId;
	
	private String cabinetCode;

	private int seqNum;

	private int type;

	private String mark = "";

	private int userId;

	private int holdId;

	private String barcode;

	private Phase phase;

	private LocalDate dueDate;

	private int siteId;

}
