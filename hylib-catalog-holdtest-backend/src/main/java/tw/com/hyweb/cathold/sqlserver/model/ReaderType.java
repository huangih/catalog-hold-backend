package tw.com.hyweb.cathold.sqlserver.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class ReaderType implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5603861327272820283L;

	@Id
	@Column(name="readTypeId")
	private int readerTypeId;

	private String readerTypeCode;

	private String readerTypeName;

}
