package tw.com.hyweb.cathold.model.client;

import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NoticeProp implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1585462432880807781L;

	private String code;

	private int id;

	private int type;

	public NoticeProp(String s) {
		String[] ss = s.split("#");
		this.code = ss[0];
		if (ss.length > 1)
			try {
				this.type = Integer.parseInt(ss[1]);
			} catch (Exception e) {
				// nothing
			}
	}
}
