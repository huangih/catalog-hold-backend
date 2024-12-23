package tw.com.hyweb.cathold.model.client;

import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TouchResult implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -234047534303820956L;

	private char type;

	private String seqResult;

	private String noticeResult = "";

	private String helpMessage = "";

	private Class<?> resultClass;

	private Serializable resultObject;

	public TouchResult(char type) {
		this.type = type;
	}

	public TouchResult(char type, String seqString) {
		this.type = type;
		this.seqResult = seqString;
	}

	public TouchResult(char type, String seqString, String noticeResult) {
		this.type = type;
		this.seqResult = seqString;
		this.noticeResult = noticeResult;
	}

	public TouchResult(char type, Serializable object) {
		this.type = type;
		this.resultClass = object.getClass();
		this.resultObject = object;
	}

}
