package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LendLog2 implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 356987153429514663L;

	@Id
	private int id;

	private int holdId;

	private int readerId;

	private String status;
	
	private char lastType;

	private char lendType;

	private String callTypes;

	private int muserId;
	
	private int preCheck;

	private LocalDateTime begTime;

	private int callbackTime;

	private int finishTime;

	private LocalDateTime updateTime;

	public LendLog2(LendCallback lendCallback) {
		BeanUtils.copyProperties(lendCallback, this);
	}
}
