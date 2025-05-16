package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import org.springframework.beans.BeanUtils;

import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.TransitOverdaysStatic;

@Data
@NoArgsConstructor
public class TransitOverdaysStaticView implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5838455304024877453L;

	private String siteCode;

	private int needFind1;

	private int notFind1;

	private int rate1;

	private int needFind2;

	private int notFind2;

	private int rate2;

	private int needFind3;

	private int notFind3;

	private int rate3;

	private int notMissing;

	private int rateM;

	public TransitOverdaysStaticView(TransitOverdaysStatic tos) {
		BeanUtils.copyProperties(tos, this);
		if (this.needFind1 != 0)
			this.rate1 = 1000 - (this.notFind1 * 1000 / this.needFind1);
		if (this.needFind2 != 0)
			this.rate2 = 1000 - (this.notFind2 * 1000 / this.needFind2);
		if (this.needFind3 != 0)
			this.rate3 = 1000 - (this.notFind3 * 1000 / this.needFind3);
		this.rateM = 1000 - rate3;
	}

}
