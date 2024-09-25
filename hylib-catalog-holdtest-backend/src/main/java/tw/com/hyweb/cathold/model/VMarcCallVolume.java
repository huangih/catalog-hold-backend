package tw.com.hyweb.cathold.model;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Table("vmarc_call_volume")
public class VMarcCallVolume implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8488093126011765025L;

	@Id
	private int id;

	private int marcId;

	private String callVolume;

	private AppendixStatus appendixStatus;

	private String volKey;

	private String catalogFormatId;

	private String classNo;

	private String authorNo;

	private String classType;

	private boolean notHotBooking;

	private boolean hotBooking;

	private boolean hotStatus;

}
