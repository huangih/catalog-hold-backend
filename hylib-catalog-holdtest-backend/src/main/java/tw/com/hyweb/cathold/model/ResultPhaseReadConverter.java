package tw.com.hyweb.cathold.model;

import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class ResultPhaseReadConverter implements Converter<String, ResultPhase> {

	@Override
	public ResultPhase convert(String phase) {
		if (phase == null || "".equals(phase))
			return ResultPhase.SYSTEM_EXCEPTION;
		return Stream.of(ResultPhase.values()).filter(s -> s.getName().equals(phase)).findFirst()
				.orElseThrow(IllegalArgumentException::new);
	}

}
