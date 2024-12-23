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
@Table(name = "reader_info")
@Data
@NoArgsConstructor
public class ReaderInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1787404873064168722L;

	@Id
	private int readerId;

	private String readerCode;
	
	private String readerName;

	private String readerSex;
	
	@Column(name = "readertypeid")
	private int readerTypeId;

	private LocalDateTime birth;

	private boolean homedeliverytype;
	
	private boolean storetype;
	
}
