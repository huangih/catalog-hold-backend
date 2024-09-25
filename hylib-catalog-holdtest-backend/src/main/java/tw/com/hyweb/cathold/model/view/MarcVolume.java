package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.AppendixStatus;
import tw.com.hyweb.cathold.model.MarcCallVolume;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarcVolume implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7546796104249721200L;

	private int marcId;

	private String callVolume = "";

	private AppendixStatus appendixStatus = AppendixStatus.NONE;

	public MarcVolume(MarcCallVolume mcv) {
		this.marcId = mcv.getMarcId();
		this.callVolume = mcv.getCallVolume();
		this.appendixStatus = mcv.getAppendixStatus();
	}

}