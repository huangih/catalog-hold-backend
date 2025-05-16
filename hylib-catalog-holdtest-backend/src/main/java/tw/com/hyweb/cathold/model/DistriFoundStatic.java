package tw.com.hyweb.cathold.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class DistriFoundStatic implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6431899167840107730L;

	private String siteCode;

	private int foundNum;

	private int distriNum;

	private int foundRate;

	private int totalDays;

	private int avgDays;

	public DistriFoundStatic(String siteCode) {
		this.siteCode = siteCode;
	}

	public DistriFoundStatic computeRate(int num) {
		this.distriNum += num;
		this.foundRate = foundNum * 1000 / distriNum;
		this.avgDays = this.totalDays * 1000 / distriNum;
		return this;
	}

}
