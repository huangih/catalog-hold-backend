package tw.com.hyweb.cathold.model;

import java.nio.ByteBuffer;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class RuleStatusWriteConverter implements Converter<RuleStatus, byte[]> {

	@Override
	public byte[] convert(RuleStatus ruleStatus) {
		int n = 0;
		if (ruleStatus != null)
			n = ruleStatus.getStatus();
		return ByteBuffer.allocate(4).putInt(n).array();
	}

}
