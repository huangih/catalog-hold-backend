package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NoticeSmsRule implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8846790859038352379L;

	@Id
	private int id;

	private LocalDate begDate;

	private LocalDate endDate;

	private String fromPSiteCode;

	private String descPSite;

	private String toMobileMsg;

	private String toPSiteCode;

	private LocalDateTime createTime = LocalDateTime.now();

	private LocalDateTime updateTime = LocalDateTime.now();

	public NoticeSmsRule(int ruleId) {
		this.id = ruleId;
	}

	public NoticeSmsRule(LocalDate begDate, LocalDate endDate, String fromPSiteCode, String descPSite,
			String toMobileMsg, String toPSiteCode) {
		this.begDate = begDate;
		this.endDate = endDate;
		this.fromPSiteCode = fromPSiteCode;
		this.descPSite = descPSite;
		this.toMobileMsg = toMobileMsg;
		this.toPSiteCode = toPSiteCode;
	}

}
