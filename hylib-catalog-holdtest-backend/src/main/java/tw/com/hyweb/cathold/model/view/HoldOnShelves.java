package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.VHoldItem;

@Data
@NoArgsConstructor
public class HoldOnShelves implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8488585959559881034L;

	private VHoldItem vHoldItem;

	private boolean canCheckout;

	private boolean onCloseShelves;

	public HoldOnShelves(VHoldItem vHoldItem) {
		this.vHoldItem = vHoldItem;
		this.canCheckout = vHoldItem.canCheckOut();
		this.onCloseShelves = vHoldItem.onCloseShelves();
	}
}
