package tw.com.hyweb.cathold.model;

import java.io.Serializable;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientSequence implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4045633801357777442L;

	@Id
	private int id;

	private int seqNum;

	public ClientSequence(int id) {
		this.id = id;
	}

}
