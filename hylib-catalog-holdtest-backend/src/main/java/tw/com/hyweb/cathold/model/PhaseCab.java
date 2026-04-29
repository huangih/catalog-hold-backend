package tw.com.hyweb.cathold.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
public enum PhaseCab { // 取書櫃位狀況
	NONE(""), NOTHING("OK") /* 空櫃 */, AVAIL_TAKE("AT")/* 待預約者取書 */, ALL_OVERDUE("RM")/* 櫃內所有資料逾期待撤 */,
	PART_OVERDUE("PO")/* 僅部分資料逾期可撤 */, CABINET_FAIL("FA")/* 取書櫃故障 */, ERROR("ER")/* 書櫃圖書資料有錯,須核萟 */;

	@Getter
	private String name;
}
