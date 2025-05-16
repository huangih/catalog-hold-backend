package tw.com.hyweb.cathold.sqlserver.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hold")
@Data
@NoArgsConstructor
public class SqlserverHoldStatus implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3796968305444963763L;

	@Id
	private int holdId;

	@Column(name = "collection_id")
	private int typeId;

	@Column(name = "keepsite_id")
	private int siteId;

	@Column(name = "keeproom_id")
	private int locationId;

	private String status;

	@Column(name = "statusupdateid")
	private int statusUpdateId = 500;

	@Column(name = "statusupdatedate")
	private LocalDateTime statusUpdateDate = LocalDateTime.now();

}
