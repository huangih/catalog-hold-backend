package tw.com.hyweb.cathold.model.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.AppendixStatus;
import tw.com.hyweb.cathold.model.MarcCallVolume;
import tw.com.hyweb.cathold.model.VHoldItem;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class MarcHoldSummary extends HoldSummary {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5055063384265539197L;

	private static final List<AppendixStatus> SPECIAL_STATUS = Arrays.asList(AppendixStatus.PERIODMERGE);

	private int callVolNum;

	private String callNumString;

	private int waitBookingNum;

	private List<Integer> callVolIds = new ArrayList<>();

	private List<Integer> pmCallVolIds = new ArrayList<>();

	private List<MarcCallVolume> marcCallVolumes;

	public MarcHoldSummary(int id, List<VHoldItem> vhis, List<MarcCallVolume> mcvs) {
		super(id, vhis);
		mcvs.stream().forEach(mcv -> {
			if (SPECIAL_STATUS.contains(mcv.getAppendixStatus()))
				this.pmCallVolIds.add(mcv.getId());
			else
				this.callVolIds.add(mcv.getId());
		});
		this.callVolNum = this.callVolIds.size() + this.pmCallVolIds.size();
		if (!mcvs.isEmpty()) {
			var mcv = mcvs.getFirst();
			this.callNumString = mcv.getClassNo() + " " + mcv.getAuthorNo();
		}
	}

	public MarcHoldSummary(int marcId) {
		super();
		this.setId(marcId);
	}

}
