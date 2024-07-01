package tw.com.hyweb.cathold.model.client;

import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SeqRange implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2342554011819981055L;

	private int minNum = 1;

	private int maxNum = 100;

	private int type;

	public SeqRange(String s) {
		String[] ss = s.split("[-#]");
		try {
			this.minNum = Integer.parseInt(ss[0]);
			this.maxNum = Integer.parseInt(ss[1]);
			if (ss.length > 2)
				this.type = Integer.parseInt(ss[2]);
		} catch (Exception e) {
			// nothing
		}
	}
}
