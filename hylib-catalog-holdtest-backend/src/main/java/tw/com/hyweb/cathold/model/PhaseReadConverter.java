package tw.com.hyweb.cathold.model;

import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class PhaseReadConverter implements Converter<String, Phase> {

	@Override
	public Phase convert(String phase) {
		if (phase == null || "".equals(phase))
			return Phase.NONE;
		return Stream.of(Phase.values()).filter(s -> s.getName().equals(phase)).findFirst()
				.orElseThrow(IllegalArgumentException::new);
	}

}
