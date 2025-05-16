package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Table("vcallvol_booking")
public class VCallvolBooking implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 535383038469175981L;

	@Id
	private long id;

	private int userId;

	private int itemId;

	private int pickupSiteId;

	private Phase phase;

	private int associateId;

	private LocalDateTime placeDate;

	private LocalDateTime distributeDate;

	private LocalDateTime transitDate;

	private LocalDateTime availableDate;

	private LocalDate dueDate;

	private int oldId;

	private int muserId;

	private LocalDateTime createTime;

	private LocalDateTime updateTime;

}
