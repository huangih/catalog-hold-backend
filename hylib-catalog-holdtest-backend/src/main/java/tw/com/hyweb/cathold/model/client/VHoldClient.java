package tw.com.hyweb.cathold.model.client;

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VHoldClient {

	private int id;
	
	private SeqRange availSeqRange;

	private List<Integer> noIntransitSites;

	private GiveSeqProp giveSeqProp;
	
	private Map<Integer, NoticeProp> noticeSitesMap;

	private Map<Integer, NoticeProp> noticeTypesMap;

	private Map<Integer, NoticeProp> noticeLocsMap;

	private boolean transitDouble;
	
	public VHoldClient(int id, boolean b) {
		this.id = id;
		this.transitDouble = b;
	}
}
