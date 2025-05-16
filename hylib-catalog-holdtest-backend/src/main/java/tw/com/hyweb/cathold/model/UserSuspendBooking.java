package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserSuspendBooking implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4538457578532472449L;

	@Id
	private int id;

	@Column
	private int userId;

	@Column
	private LocalDate begDate;

	@Column
	private LocalDate endDate;

	private SuspendPhase phase = SuspendPhase.SUSPEND_PLACE;

	private int muserId;

	private LocalDateTime createTime;

	private LocalDateTime updateTime;

}
