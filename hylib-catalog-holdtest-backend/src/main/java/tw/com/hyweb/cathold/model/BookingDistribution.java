package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BookingDistribution implements Serializable {
	/**
	* 
	*/
	private static final long serialVersionUID = -2567501389524351969L;

	@Id
	private int holdId;

	private int callVolId;

	private int dueSite;

	private String bookingType;

	private LocalDateTime beginDate = LocalDateTime.now();

	private int days = 1;

	private LocalDateTime createTime = LocalDateTime.now();

	private LocalDateTime updateTime = LocalDateTime.now();

}
