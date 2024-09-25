package tw.com.hyweb.cathold.sqlserver.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lendfile")
@NamedQuery(name = "SqlserverCharged.findAll", query = "SELECT h FROM SqlserverCharged h")
@Data
@NoArgsConstructor
public class SqlserverCharged implements Serializable {
	/**
	* 
	*/
	private static final long serialVersionUID = -5242906910541731109L;

	@Id
	private long lenId;

	private int holdId;

	private int readerId;

	@Column(name = "lenddate")
	private LocalDateTime lendDate;
	
	@Column(name = "returndate")
	private LocalDateTime returnDate;
	
}
