package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.time.LocalDate;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.hyweb.cathold.model.VHoldItem;

@Data
@NoArgsConstructor
public class HoldOnOther implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8488585959559881034L;

	private VHoldItem vHoldItem;

	@JsonSerialize(using = LocalDateSerializer.class)
	@JsonDeserialize(using = LocalDateDeserializer.class)
	private LocalDate dueReturnDate;

	private IntransitView intransit;

	public HoldOnOther(VHoldItem vHoldItem) {
		this.vHoldItem = vHoldItem;
	}

}