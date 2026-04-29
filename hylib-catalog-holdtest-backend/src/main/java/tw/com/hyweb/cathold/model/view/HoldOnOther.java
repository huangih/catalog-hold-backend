package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.VHoldItem;

@Data
@NoArgsConstructor
public class HoldOnOther implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8488585959559881034L;

	private VHoldItem vHoldItem;

	private LocalDate dueReturnDate;

	private IntransitView intransit;

	public HoldOnOther(VHoldItem vHoldItem) {
		this.vHoldItem = vHoldItem;
	}

}
