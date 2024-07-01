package tw.com.hyweb.cathold.model;

import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class AppendixStatusReadConverter implements Converter<String, AppendixStatus> {

	@Override
	public AppendixStatus convert(String status) {
		if (status == null)
			return AppendixStatus.NONE;
		return Stream.of(AppendixStatus.values()).filter(s -> s.getStatus().equals(status)).findFirst()
				.orElseThrow(IllegalArgumentException::new);
	}

}
