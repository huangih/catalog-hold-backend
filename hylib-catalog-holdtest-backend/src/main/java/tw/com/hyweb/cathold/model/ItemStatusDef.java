package tw.com.hyweb.cathold.model;

import java.io.Serializable;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ItemStatusDef implements Serializable {

	/**
		 * 
		 */
	private static final long serialVersionUID = 4139475951286349498L;

	@Id
	private int statusId;

	private String statusCode;

	private String statusName;

}
