package tw.com.hyweb.cathold.model;

import java.io.Serializable;

import org.springframework.data.annotation.Id;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MessageMap implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2546631924030718565L;

	@Id
	private int id;

	private String type;

	private String phase;

	private String code;

}
