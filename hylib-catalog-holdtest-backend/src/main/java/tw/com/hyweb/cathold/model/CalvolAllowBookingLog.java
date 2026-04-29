package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CalvolAllowBookingLog implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4239720505045565169L;

	@Id
	private int id;

	private int callVolId;

	private String holdIds;

	private LocalDateTime createTime = LocalDateTime.now();

	public CalvolAllowBookingLog(int callVolId, String holdIds) {
		this.callVolId = callVolId;
		this.holdIds = holdIds;
	}
}
