package tw.com.hyweb.cathold.model;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class PhaseCabWriteConverter implements Converter<PhaseCab, String> {

	@Override
	public String convert(PhaseCab phaseCab) {
		if (phaseCab == null)
			return "";
		return phaseCab.getName();
	}

}
