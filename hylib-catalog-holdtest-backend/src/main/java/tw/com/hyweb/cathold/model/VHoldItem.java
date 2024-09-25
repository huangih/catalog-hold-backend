package tw.com.hyweb.cathold.model;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Table("vhold_item")
public class VHoldItem implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1464592070261547079L;

	@Id
	private int holdId;

	private String barcode;

	private int marcId;

	private int callVolId;

	private String volume;

	private int marcVolId;

	private AppendixStatus appendixStatus;

	private int itslId;

	private int typeId;

	private String typeCode;

	private int siteId;

	private String siteCode;

	private int locId;

	private Integer keeproomId;

	private String locCode;

	private int statusId;

	@Column( "status")
	private String statusCode;

	private int tempStatus;

	private RuleStatus ruleStatus;

	public boolean nonShadow() {
		return this.ruleStatus.nonShadow();
	}

	public boolean onShelves() {
		return this.ruleStatus.onShelves();
	}

	public boolean allowBooking() {
		return this.ruleStatus.allowBooking();
	}

	public boolean onCheckout() {
		return this.ruleStatus.onCheckout();
	}

	public boolean onOther() {
		return !(this.onShelves() || this.onCheckout());
	}

	public boolean onshelvesHold() {
		return this.ruleStatus.onshelvesHold();
	}

	public boolean canCheckOut() {
		return this.ruleStatus.canCheckOut();
	}

	public boolean onCloseShelves() {
		return this.ruleStatus.onCloseShelves();
	}

	public boolean ruleDepdentUser() {
		return this.ruleStatus.ruleDepdentUser();
	}

	public boolean bookingCheckOut() {
		return this.ruleStatus.bookingCheckOut();
	}

	public boolean supportBooking() {
		return this.ruleStatus.supportBooking();
	}

	public boolean floatItem() {
		return this.ruleStatus.floatItem();
	}

	public void setPropertyStatus(int type, boolean b) {
		this.ruleStatus.setPropertyStatus(type, b);
	}

}
