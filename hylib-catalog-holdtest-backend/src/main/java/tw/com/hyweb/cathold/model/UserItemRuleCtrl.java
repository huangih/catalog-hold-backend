package tw.com.hyweb.cathold.model;

import java.io.Serializable;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserItemRuleCtrl implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4058459743395306887L;

	@Id
	private int id;
	
	private int itslId;

	private int statusId;
	
	private int type;

	private int userRule;
}
