package tw.com.hyweb.cathold.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleStatus implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 608728280326017852L;

	private int status;

	public boolean onshelvesHold() {
		int n = (this.status >> 23) & 0x5;
		return this.bookingCheckOut() && n == 0x5;
	}

	public boolean onShelves() {
		return (this.status & (1 << 23)) > 0;
	}

	public boolean nonShadow() {
		return this.status >= 0;
	}

	public boolean allowBooking() {
		if (this.status < 0)
			return false;
		int rs = this.status >> 24;
		if ((rs & 0x20) == 0)
			return false;
		return (rs & 0x0A) > 0;
	}

	public boolean onCheckout() {
		return (this.status & (1 << 21)) > 0;
	}

	public boolean canCheckOut() {
		return (this.status & (1 << 29)) > 0;
	}

	public boolean onCloseShelves() {
		return (this.status & (1 << 19)) > 0;
	}

	public boolean supportBooking() {
		return (this.status & (1 << 17)) > 0;
	}

	public boolean bookingCheckOut() {
		return (this.status & (3 << 28)) > 0;
	}

	public boolean floatItem() {
		return (this.status & (1 << 15)) > 0;
	}

}
