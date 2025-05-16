package tw.com.hyweb.cathold.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
public enum SuspendPhase {
	SUSPEND_PLACE("P")/* 暫停分配設定 */, ON_SUSPENSION("S")/* 暫停預約分配中 */, END_SUSPENSION("D")/* 結束暫停 */,
	CANCEL_SUSPENCION("C")/* 取消暫停預約分配 */;

	@Getter
	private String phase;
}
