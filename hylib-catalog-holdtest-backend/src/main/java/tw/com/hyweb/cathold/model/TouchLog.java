package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TouchLog implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8818680655493060740L;

	@Id
	private int id;

	private String barcode;

	private int clientId;

	private String result = "";

	private long preMillions;

	private int preLast;

	private int number;

	private long millions;

	private String seqResult;

	private String status;

	private LocalDateTime createTime = LocalDateTime.now();

	public TouchLog(String barcode, int clientId) {
		this.barcode = barcode;
		this.clientId = clientId;
	}

}
