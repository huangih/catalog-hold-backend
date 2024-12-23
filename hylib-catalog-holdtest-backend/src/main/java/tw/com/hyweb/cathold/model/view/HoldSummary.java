package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.VHoldItem;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public abstract class HoldSummary implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8321422172869053878L;

	@Include
	private int id;

	protected int holdNum;

	protected int onShelveNum;

	protected int allowBookingNum;

	protected int otherNum;

	protected HoldSummary(int id, List<VHoldItem> holdItems) {
		this.id = id;
		this.holdNum = holdItems.size();
		this.onShelveNum = (int) holdItems.stream().filter(VHoldItem::onShelves).count();
		this.allowBookingNum = (int) holdItems.stream().filter(VHoldItem::allowBooking).count();
		this.otherNum = (int) holdItems.stream().filter(VHoldItem::onOther).count();
	}
}
