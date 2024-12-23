package tw.com.hyweb.cathold.model.view;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.MarcCallVolume;
import tw.com.hyweb.cathold.model.VHoldItem;
import tw.com.hyweb.cathold.model.VHotBookingDate;
import tw.com.hyweb.cathold.sqlserver.model.SqlserverCharged;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class CallVolHoldSummary extends HoldSummary {
	/**
	* 
	*/
	private static final long serialVersionUID = -1139986531319921132L;

	private MarcVolume marcVolume;

	private boolean hotBooking;

	private LocalDate statusDate;

	private int waitBookingNum;

	private int checkoutNum;

	private List<VHoldItem> onShelveHolds = new ArrayList<>();

	private List<VHoldItem> onChargedHolds = new ArrayList<>();

	private List<VHoldItem> onOtherHolds = new ArrayList<>();

	private Map<Integer, LocalDate> chargedDueDateMap;

	private LocalDate recentDueDate;

	public CallVolHoldSummary(int id, List<VHoldItem> vhis) {
		super(id, vhis);
		this.onShelveHolds = vhis.stream().filter(VHoldItem::onShelves).toList();
		this.onChargedHolds = vhis.stream().filter(VHoldItem::onCheckout).toList();
		this.checkoutNum = this.onChargedHolds.size();
		this.onOtherHolds = vhis.stream().filter(VHoldItem::onOther).toList();
	}

	public void setChargedDueDates(List<SqlserverCharged> sqlserverChargeds) {
		this.recentDueDate = sqlserverChargeds.get(0).getLendDate().toLocalDate();
		this.chargedDueDateMap = sqlserverChargeds.stream()
				.collect(Collectors.toMap(SqlserverCharged::getHoldId, s -> s.getReturnDate().toLocalDate()));
	}

	public List<HoldOnCharged> getChargedDueDates(List<HoldOnCharged> holdOnChargeds) {
		List<HoldOnCharged> list = new ArrayList<>();
		holdOnChargeds.forEach(hoc -> {
			int holdId = hoc.getVHoldItem().getHoldId();
			if (this.chargedDueDateMap.containsKey(holdId)) {
				hoc.setDueDate(this.chargedDueDateMap.get(holdId));
				list.add(hoc);
			}
		});
		return list;
	}

	public boolean isRenewableLend(int overNum, int waitBookingNum) {
		int num = (int) this.onShelveHolds.stream().filter(VHoldItem::allowBooking).count();
		return (num - waitBookingNum) >= overNum || waitBookingNum == 0;
	}

	public void setHotBookingDate(VHotBookingDate vHotBookingDate) {
		if (vHotBookingDate != null && vHotBookingDate.isHotBooking()) {
			this.hotBooking = true;
			this.statusDate = vHotBookingDate.getStatusDate();
		} else {
			this.hotBooking = false;
			this.statusDate = null;
		}

	}

	public CallVolHoldSummary(int id, List<VHoldItem> vhis, MarcCallVolume mcv, List<SqlserverCharged> lendLi) {
		super(id, vhis);
		this.onShelveHolds = vhis.stream().filter(VHoldItem::onShelves).toList();
		this.onChargedHolds = vhis.stream().filter(VHoldItem::onCheckout).toList();
		this.checkoutNum = this.onChargedHolds.size();
		this.onOtherHolds = vhis.stream().filter(VHoldItem::onOther).toList();
		this.marcVolume = new MarcVolume(mcv);
		this.hotBooking = mcv.isHotBooking();
		if (lendLi != null && !lendLi.isEmpty()) {
			this.recentDueDate = lendLi.get(0).getReturnDate().toLocalDate();
			this.chargedDueDateMap = lendLi.stream()
					.collect(Collectors.toMap(SqlserverCharged::getHoldId, lend -> lend.getReturnDate().toLocalDate()));
		}
	}
	
	public void updateHoldItemsStatus(List<VHoldItem> vHoldItems) {
		this.holdNum = vHoldItems.size();
		this.onShelveNum = (int) vHoldItems.stream().filter(VHoldItem::onShelves).count();
		this.allowBookingNum = (int) vHoldItems.stream().filter(VHoldItem::allowBooking).count();
		this.otherNum = (int) vHoldItems.stream().filter(VHoldItem::onOther).count();
		this.onShelveHolds = vHoldItems.stream().filter(VHoldItem::onShelves).toList();
		this.onChargedHolds = vHoldItems.stream().filter(VHoldItem::onCheckout).toList();
		this.checkoutNum = this.onChargedHolds.size();
		this.onOtherHolds = vHoldItems.stream().filter(VHoldItem::onOther).toList();
	}

}
