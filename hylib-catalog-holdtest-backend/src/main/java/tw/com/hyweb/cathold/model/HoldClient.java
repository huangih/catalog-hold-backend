package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldClient implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2808267707300096116L;

	@Id
	private int id;

	private String siteCode;

	private String name;

	private String noIntransitSites;

	private String giveSeqProp;

	private String seqRange;

	private String noticeSites;

	private String noticeTypes;

	private String noticeLocations;

	private boolean transitDouble;

	private boolean toFloatLoc;
	
	@Transient
	@Builder.Default
	private int currentSeq = 0;

	@Builder.Default
	private LocalDateTime createTime = LocalDateTime.now();

	@Builder.Default
	private LocalDateTime updateTime = LocalDateTime.now();

	public HoldClient(int id) {
		this.id = id;
	}

}
