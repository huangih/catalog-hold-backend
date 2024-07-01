package tw.com.hyweb.cathold.model;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class PhaseWriteConverter implements Converter<Phase, String> {

	@Override
	public String convert(Phase phase) {
		if (phase == null)
			return "";
		return phase.getName();
	}

}
