package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.VHoldItem;

@Data
@NoArgsConstructor
public class HoldOnCharged implements Serializable {
	/**
	* 
	*/
	private static final long serialVersionUID = 7931163743679056719L;

	private VHoldItem vHoldItem;

	private LocalDate dueDate;

	public HoldOnCharged(VHoldItem vHoldItem) {
		this.vHoldItem = vHoldItem;
	}
}
