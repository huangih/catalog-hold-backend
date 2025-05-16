package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class NoticeProp implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6148400913282737967L;

	private int id;

	private int userId;

	private int pickupSiteId;

	LocalDate availableDate;

	private char type;

	private int number;

	private LocalDateTime createTime;

	private LocalDateTime updateTime;

}
