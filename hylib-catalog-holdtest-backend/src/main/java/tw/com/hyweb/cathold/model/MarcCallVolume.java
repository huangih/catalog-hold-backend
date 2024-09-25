package tw.com.hyweb.cathold.model;

import java.io.Serializable;

import org.springframework.beans.BeanUtils;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MarcCallVolume implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8488093126011765025L;

	private int id;

	private int marcId;

	private String callVolume;

	private AppendixStatus appendixStatus;

	private String volKey;

	private String classNo;

	private String authorNo;

	private boolean notHotBooking;

	private boolean hotBooking;

	private boolean hotStatus;

	public MarcCallVolume(VMarcCallVolume vMcv) {
		BeanUtils.copyProperties(vMcv, this);
	}

}
