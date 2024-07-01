package tw.com.hyweb.cathold.model;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class RuleStatusReadConverter implements Converter<byte[], RuleStatus> {

	@Override
	public RuleStatus convert(byte[] dbData) {
		int n = 0;
		if (dbData != null)
			for (int i = 0; i < dbData.length; i++) {
				n <<= 8;
				n += Byte.toUnsignedInt(dbData[i]);
			}
		RuleStatus rs = new RuleStatus();
		rs.setStatus(n);
		return rs;
	}

}
