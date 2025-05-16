package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.AbstractAggregateRoot;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor
public class NoticeResult extends AbstractAggregateRoot<NoticeResult> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7636404123120197102L;

	@Id
	private int id;

	@Include
	private int noticeId;

	@Include
	private String type;

	@Include
	private int number;

	private LocalDateTime startTime = LocalDateTime.now();

	private LocalDateTime statusTime = LocalDateTime.now();

	private String lastStatus;

	private String result;

	private String descResult;

	private int joinId;

	private LocalDateTime createTime = LocalDateTime.now();

	private LocalDateTime updateTime = LocalDateTime.now();

	public void newEvent() {
		registerEvent(this);
	}

	public NoticeResult(NoticeProp noticeProp) {
		this.noticeId = noticeProp.getId();
		this.type = String.valueOf(noticeProp.getType());
		this.number = noticeProp.getNumber();
	}

	public NoticeResult(int noticeId, char type, int number) {
		this.noticeId = noticeId;
		this.type = String.valueOf(type);
		this.number = number;
	}

}
