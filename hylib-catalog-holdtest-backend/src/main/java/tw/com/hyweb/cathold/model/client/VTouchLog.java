package tw.com.hyweb.cathold.model.client;

import java.io.Serializable;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Table("vtouch_log")
public class VTouchLog implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4832085457766968590L;

	@Id
	private int id;

	private String barcode;

	private String siteCode;

	private char result;

	private String status;

	private LocalDateTime createTime;

}
