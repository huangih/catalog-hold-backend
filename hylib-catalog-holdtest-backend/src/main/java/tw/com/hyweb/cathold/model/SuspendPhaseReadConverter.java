package tw.com.hyweb.cathold.model;

import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class SuspendPhaseReadConverter implements Converter<String, SuspendPhase> {

	@Override
	public SuspendPhase convert(String dbData) {
		if (dbData == null)
			return SuspendPhase.SUSPEND_PLACE;
		return Stream.of(SuspendPhase.values()).filter(s -> Objects.equals(s.getPhase(), dbData)).findFirst()
				.orElseThrow(IllegalArgumentException::new);
	}

}
