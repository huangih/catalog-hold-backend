package tw.com.hyweb.cathold.model;

import java.io.Serializable;

import org.springframework.data.annotation.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ItemTypeDef implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private int itemTypeId;

	private String itemTypeCode;

	private String itemTypeName;

	public ItemTypeDef(int typeId) {
		this.itemTypeId = typeId;
	}

	public ItemTypeDef(String code) {
		this.itemTypeCode = code;
	}

}
