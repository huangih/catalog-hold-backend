package tw.com.hyweb.cathold.model.view;

import java.time.LocalDate;

import org.springframework.beans.BeanUtils;

import lombok.Data;
import tw.com.hyweb.cathold.model.Phase;
import tw.com.hyweb.cathold.model.VBookingCabinetHold;

@Data
public class CabinetSuggestHoldView {

	private long bookingId;

	private int cabId;

	private String cabinetCode;

	private String availSeqNum;

	private int userId;

	private int holdId;

	private String barcode;

	private Phase phase;

	private LocalDate dueDate;

	public CabinetSuggestHoldView(VBookingCabinetHold vbch) {
		BeanUtils.copyProperties(vbch, this);
		this.availSeqNum = String.format("%03d", vbch.getSeqNum());
		if (!vbch.getMark().isEmpty())
			this.availSeqNum += String.format("(%s)", vbch.getMark());
		if (vbch.getType() != 0)
			this.availSeqNum += "#" + vbch.getType();
	}

}
