package tw.com.hyweb.cathold.model;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TplLocationDef {

	public TplLocationDef(String locCode, String locationName) {
		this.locationCode = locCode;
		this.locationName = locationName;
	}

	@Id
	private int id;

	private String locationCode;

	private String locationName;
}
