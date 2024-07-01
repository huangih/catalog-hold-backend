package tw.com.hyweb.cathold.model.client;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GiveSeqProp implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7544378163377179525L;

	private boolean checkSite;

	private List<String> siteCodes;

	private List<Integer> siteIds;

	private boolean checkAnnex;

	private boolean annexBooking;

	private List<String> typeCodes;

	private List<String> locCodes;

	private List<Integer> typeIds;

	private List<Integer> locIds;

	private boolean annexOrtype;

	private boolean checkType;

	private boolean containType;

	private boolean typeOrloc;

	private boolean checkLoc;

	private boolean containLoc;

	private String cutCodes(String s) {
		if ('!' == s.charAt(0))
			s = s.substring(1);
		int inx1 = s.indexOf('(');
		if (inx1 >= 0) {
			int inx2 = s.lastIndexOf(')');
			s = s.substring(++inx1, inx2);
		}
		return s;
	}

	public void setSiteProp(int inx, String s) {
		this.checkSite = true;
		this.siteCodes = Arrays.asList(s.substring(0, inx).split(","));
	}

	public void setAnnexProp(int inx, String s) {
		this.checkAnnex = true;
		String s1 = s.substring(0, inx);
		if ("Y".equalsIgnoreCase(s1))
			this.annexBooking = true;
		char c = s.length() > inx ? s.charAt(inx) : 0;
		this.annexOrtype = '^' == c;
	}

	public void setTypeProp(int inx, String s) {
		this.checkType = true;
		String s1 = s.substring(0, inx);
		if (s1.charAt(0) != '!')
			this.containType = true;
		s1 = this.cutCodes(s1);
		this.typeCodes = Arrays.asList(s1.split(","));
		char c = s.length() > inx ? s.charAt(inx) : 0;
		this.typeOrloc = '^' == c;
	}

	public void setLocProp(int inx, String s) {
		this.checkLoc = true;
		s = s.substring(0, inx);
		if (s.charAt(0) != '!')
			this.containLoc = true;
		s = this.cutCodes(s);
		this.locCodes = Arrays.asList(s.split(","));
	}

}
