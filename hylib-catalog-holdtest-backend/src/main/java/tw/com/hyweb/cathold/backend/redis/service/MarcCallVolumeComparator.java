package tw.com.hyweb.cathold.backend.redis.service;

import java.io.Serializable;
import java.util.Comparator;

import tw.com.hyweb.cathold.model.MarcCallVolume;

public class MarcCallVolumeComparator implements Comparator<MarcCallVolume>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6943668502695626802L;

	public static final Comparator<MarcCallVolume> INSTANCE = new MarcCallVolumeComparator();

	private MarcCallVolumeComparator() {
	}

	@Override
	public int compare(MarcCallVolume mcv1, MarcCallVolume mcv2) {
		if (mcv1.getId() == mcv2.getId())
			return 0;
		return Comparator.comparing(MarcCallVolume::getVolKey, Comparator.nullsFirst(Comparator.reverseOrder()))
				.thenComparing(MarcCallVolume::getAppendixStatus).compare(mcv1, mcv2);
	}

}
