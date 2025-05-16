package tw.com.hyweb.cathold.model;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class ResultPhaseWriteConverter implements Converter<ResultPhase, String> {

	@Override
	public String convert(ResultPhase phase) {
		if (phase == null)
			return "";
		return phase.getName();
	}

}
