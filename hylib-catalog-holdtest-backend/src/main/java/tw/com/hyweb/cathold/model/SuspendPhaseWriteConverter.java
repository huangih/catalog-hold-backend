package tw.com.hyweb.cathold.model;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class SuspendPhaseWriteConverter implements Converter<SuspendPhase, String> {

	@Override
	public String convert(SuspendPhase phase) {
		if (phase == null)
			return "P";
		return phase.getPhase();
	}

}
