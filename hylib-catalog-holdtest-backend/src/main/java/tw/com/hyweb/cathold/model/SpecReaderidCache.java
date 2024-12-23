package tw.com.hyweb.cathold.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.sqlserver.model.ReaderInfo;

@Data
@NoArgsConstructor
public class SpecReaderidCache implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2690492983870634913L;

	@Id
	private int readerId;

	private int readerType;

	private LocalDateTime updateTime;

	public SpecReaderidCache(ReaderInfo readerInfo) {
		this.readerId = readerInfo.getReaderId();
		this.readerType = readerInfo.getReaderTypeId();
	}
}
