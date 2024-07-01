package tw.com.hyweb.cathold.model;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class AppendixStatusWriteConverter implements Converter<AppendixStatus, String> {

	@Override
	public String convert(AppendixStatus appendixStatus) {
		if (appendixStatus == null)
			return null;
		return appendixStatus.getStatus();
	}

}
