package tw.com.hyweb.cathold.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
public enum Phase {
	NONE(" "), PLACE("P")/* 預約開始 */, SUSPENSION("S")/* 暫停預約分配 */, DISTRIBUTION("D")/* 分配架上找書中 */,
	ITEMMISSING("M")/* 遺失 */, NOMORENEED("N")/* 所尋獲預約書已無預約者 */, NONE_DISTRIBUTE("ND")/* 分配之預約書被借 */,
	WAIT_TRANSITB("WT")/* SIP2等調撥確認 */, WAIT_TRANSITR("WR")/* 調撥待確認 */, TRANSIT_B("TB")/* 調撥至預約取書館 */,
	TRANSITB_LEND("TL")/* 預約調撥被借閱 */, TRANSIT_RELAY("RT")/* 巡迴車調撥中 */, TRANSIT_WA("TW")/* 調撥暫停待附件 */,
	TRANSIT_R("TR")/* 調撥回館 */, TRANSIT_SUSPENSION("ST")/* 調撥中暫停分配 */, WAIT_ANNEX("WA")/* 等待附件到館 */,
	AVAILABLE("A")/* 預約到館可借 */, CANCELED("C")/* 預約取消 */, A01_ORDER("AO")/* 超商取書下訂 */, LEND_CANCEL("LC")/* 預約中先行借閱 */,
	NOSAME_USER_LEND("NL")/* 非預約者借出 */, ANNEX_MISSING("AM")/* 到館預約所需之附件遺失 */, CHG_PICKUPSITE_AVAIL("CP")/* 到館後變更取書館 */,
	FILLED("BF")/* 預約者己借 */, OVERDUE_BOOKING_WAITING("BW")/* 預約未取待確認 */, OVERDUE_CANCEL("OC")/* 取消預約未取記點 */,
	OVERDUE_BOOKING("BV")/* 預約未取 */, ON_STOP_BOOKING("SB")/* 停止預約中 */, END_STOP_BOOKING("EB")/* 已結束之暫停預約 */,
	OVERDUE_OVER_AVAIL("OO")/* 預約未取記點效期已過 */;

	@Getter
	private String name;
}
