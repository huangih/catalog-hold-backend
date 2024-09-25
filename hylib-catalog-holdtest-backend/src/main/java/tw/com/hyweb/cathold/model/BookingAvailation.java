package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.annotation.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class BookingAvailation implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1546586677884356659L;

	private static final List<String> notExpDuedateMark = Arrays.asList("H");

	@Id
	@Include
	private long bookingId;

	private int seqNum;

	private int type;

	private String mark = "";

	private int noticeId;

	private LocalDateTime createTime = LocalDateTime.now();

	private LocalDateTime updateTime = LocalDateTime.now();

	public BookingAvailation(long bookingId, int seqNum, int type) {
		this.bookingId = bookingId;
		this.seqNum = seqNum;
		this.type = type;
	}

	public BookingAvailation(long bookingId) {
		this.bookingId = bookingId;
	}

	public BookingAvailation(long bookingId, int type) {
		this.bookingId = bookingId;
		this.type = type;
	}

	public String getAvailSeqNum() {
		String s = String.format("%03d", this.seqNum);
		if (this.mark.length() > 0)
			s += String.format("(%s)", this.mark);
		if (this.type != 0)
			s += "#" + this.type;
		return s;
	}

	public boolean isExpDuedateMark() {
		return !notExpDuedateMark.contains(this.mark);
	}

}
