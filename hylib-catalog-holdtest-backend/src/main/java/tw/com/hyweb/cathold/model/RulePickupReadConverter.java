package tw.com.hyweb.cathold.model;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class RulePickupReadConverter implements Converter<byte[], Integer> {

	@Override
	public Integer convert(byte[] dbData) {
		int n = 0;
		if (dbData != null)
			for (int i = 0; i < dbData.length; i++) {
				n <<= 8;
				n += Byte.toUnsignedInt(dbData[i]);
			}
		return n;
	}

}
