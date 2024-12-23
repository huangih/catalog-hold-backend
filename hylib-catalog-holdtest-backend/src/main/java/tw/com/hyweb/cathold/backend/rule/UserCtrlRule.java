package tw.com.hyweb.cathold.backend.rule;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.springframework.util.MethodInvoker;

import lombok.Data;

@Data
public class UserCtrlRule {

	private final MethodInvoker readerMethod;

	private final MethodInvoker checkMethod;

	private final Class<?>[] classes;

	private final List<String> expList;

	private final boolean retBoolean;

	public Object invokeReaderMethod(int readerId)
			throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException {
		this.readerMethod.setArguments(readerId);
		this.readerMethod.prepare();
		return this.readerMethod.invoke();
	}

	public Boolean invokeCheckMethod(Object[] args)
			throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		this.checkMethod.setArguments(args);
		this.checkMethod.prepare();
		boolean b = (boolean) this.checkMethod.invoke();
		return this.retBoolean ? b : !b;
	}

}
