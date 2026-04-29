package tw.com.hyweb.cathold.model;

import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class PhaseCabReadConverter implements Converter<String, PhaseCab> {

	@Override
	public PhaseCab convert(String phaseCab) {
		if (phaseCab == null || "".equals(phaseCab))
			return PhaseCab.NOTHING;
		return Stream.of(PhaseCab.values()).filter(s -> s.getName().equals(phaseCab)).findFirst()
				.orElseThrow(IllegalArgumentException::new);
	}

}
