package tw.com.hyweb.cathold.model.client;

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.HoldClient;
import tw.com.hyweb.cathold.model.ItemSiteDef;

@Data
@NoArgsConstructor
public class VHoldClient {

	private int id;

	private char floatGroup;

	private boolean floatReceive;

	private SeqRange availSeqRange;

	private List<Integer> noIntransitSites;

	private GiveSeqProp giveSeqProp;

	private Map<Integer, NoticeProp> noticeSitesMap;

	private Map<Integer, NoticeProp> noticeTypesMap;

	private Map<Integer, NoticeProp> noticeLocsMap;

	private boolean transitDouble;

	private boolean toFloatLoc;

	public VHoldClient(ItemSiteDef siteDef, HoldClient holdClient) {
		this.id = holdClient.getId();
		if (siteDef.getFloatGroup() != null)
			this.floatGroup = siteDef.getFloatGroup().charAt(0);
		this.floatReceive = siteDef.isFloatReceive();
		this.transitDouble = holdClient.isTransitDouble();
		this.toFloatLoc = holdClient.isToFloatLoc();
	}
}
