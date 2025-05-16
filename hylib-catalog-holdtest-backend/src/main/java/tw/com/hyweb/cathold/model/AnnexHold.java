package tw.com.hyweb.cathold.model;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class AnnexHold implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3803326806809622735L;

	@Id
	private int annexHoldId;

	private long seqId;

	private int holdId;

	@Transient
	private int omainId;

}
