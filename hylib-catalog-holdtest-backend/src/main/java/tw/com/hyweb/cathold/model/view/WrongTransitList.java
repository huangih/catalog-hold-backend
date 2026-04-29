package tw.com.hyweb.cathold.model.view;

import java.util.List;

import lombok.Data;
import tw.com.hyweb.cathold.model.WrongTransit;

@Data
public class WrongTransitList {

	private int total;

	private int wrongNum;

	private List<WrongTransit> wrongTransits;

	public WrongTransitList(int total, int wrongNum) {
		this.total = total;
		this.wrongNum = wrongNum;
	}

}
