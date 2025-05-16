package tw.com.hyweb.cathold.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResult implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6809223424801940360L;

	private long id;
	
	private ResultPhase resultPhase;

	private Class<?> resultClass;

	private Serializable result;

	public BookingResult(ResultPhase resultPhase) {
		this.resultPhase = resultPhase;
	}

	public BookingResult(long id, ResultPhase resultPhase) {
		this.id = id;
		this.resultPhase = resultPhase;
	}

}
