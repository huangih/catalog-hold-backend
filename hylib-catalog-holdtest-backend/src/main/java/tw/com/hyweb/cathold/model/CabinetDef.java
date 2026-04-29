package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CabinetDef implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5481708665284215360L;

	@Id
	private int id;

	private String cabCode;

	private int siteId;

	private int cabId;

	private int userId;

	private PhaseCab status;

	private LocalDateTime updateTime = LocalDateTime.now();

}
