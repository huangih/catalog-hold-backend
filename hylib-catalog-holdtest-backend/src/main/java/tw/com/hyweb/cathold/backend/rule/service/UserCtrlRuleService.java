package tw.com.hyweb.cathold.backend.rule.service;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Component;
import org.springframework.util.MethodInvoker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.backend.redis.service.VParameterService;
import tw.com.hyweb.cathold.backend.rule.UserCtrlRule;
import tw.com.hyweb.cathold.model.CatalogHoldRule;
import tw.com.hyweb.cathold.sqlserver.model.ReaderInfo;
import tw.com.hyweb.cathold.sqlserver.model.ReaderType;
import tw.com.hyweb.cathold.sqlserver.repository.ReaderInfoRepository;
import tw.com.hyweb.cathold.sqlserver.repository.ReaderTypeRepository;

@Component
@RequiredArgsConstructor
public class UserCtrlRuleService {

	private static final String ITEMUSERRULE_PREFIX = "itemUserCheckRule_";

	private final VParameterService vParameterService;

	private final ReaderInfoRepository readerInfoRepository;

	private final ReaderTypeRepository readerTypeRepository;

	private final ObjectMapper objectMapper;

	private Map<Integer, UserCtrlRule> userCtrlRuleMap;

	private Mono<Map<Integer, UserCtrlRule>> getUserCtrlRuleMap() {
		return Mono.justOrEmpty(this.userCtrlRuleMap)
				.switchIfEmpty(this.vParameterService.getRulesByLikeRuleNames(ITEMUSERRULE_PREFIX)
						.collectMap(CatalogHoldRule::getRuleSequence, this::parseUserCtrlRule).map(map -> {
							this.userCtrlRuleMap = map;
							return map;
						}));
	}

	private UserCtrlRule parseUserCtrlRule(CatalogHoldRule chr) {
		String s = chr.getRuleClassName().split("_")[1];
		List<String> expList = new ArrayList<>(Arrays.asList(chr.getRuleExp().split("\\|", -1)));
		MethodInvoker mi1 = new MethodInvoker();
		mi1.setTargetMethod("get" + s.substring(0, 1).toUpperCase() + s.substring(1));
		MethodInvoker mi2 = new MethodInvoker();
		String s1 = expList.removeFirst();
		mi2.setTargetMethod(s + s1);
		mi1.setTargetObject(this);
		mi2.setTargetObject(this);
		Class<?>[] classes = null;
		Boolean retBoolean = true;
		try {
			classes = objectMapper.readerForArrayOf(Class.class).readValue(expList.removeFirst());
			retBoolean = objectMapper.readValue(expList.removeLast(), boolean.class);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return new UserCtrlRule(mi1, mi2, classes, expList, retBoolean);
	}

	public String getUserType(int readerId) {
		try {
			ReaderInfo readerInfo = this.readerInfoRepository.findByReaderId(readerId).orElseThrow();
			ReaderType readerType = this.readerTypeRepository.findByReaderTypeId(readerInfo.getReaderTypeId())
					.orElseThrow();
			return readerType.getReaderTypeCode();
		} catch (NoSuchElementException e) {
			// nothing to do
		}
		return "";
	}

	public int getUserAge(int readerId) {
		LocalDate today = LocalDate.now();
		try {
			ReaderInfo readerInfo = this.readerInfoRepository.findById(readerId).orElseThrow();
			LocalDate birthDate = readerInfo.getBirth().toLocalDate();
			return (int) ChronoUnit.YEARS.between(birthDate, today);
		} catch (NoSuchElementException | NullPointerException e) {
			// nothing to do
		}
		return -1;
	}

	public boolean userAgeBefore(int readerAge, int age) {
		return readerAge < age;
	}

	public boolean userAgeAfter(int readerAge, int age) {
		return readerAge > age;
	}

	public boolean userTypeIn(String readerType, List<String> types) {
		return types.contains(readerType);
	}

	public Mono<Boolean> checkUserRule(int readerId, int ruleNum) {
		return this.getUserCtrlRuleMap().map(map -> map.get(ruleNum)).map(ucr -> {
			try {
				Object retObject = ucr.invokeReaderMethod(readerId);
				Object[] args = this.prepareArguments(retObject, ucr.getClasses(), ucr.getExpList());
				return ucr.invokeCheckMethod(args);
			} catch (InvocationTargetException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException
					| JsonProcessingException e) {
				e.printStackTrace();
			}
			return false;
		});
	}

	private Object[] prepareArguments(Object retObject, Class<?>[] classes, List<String> expList)
			throws JsonProcessingException {
		Object[] args = new Object[classes.length];
		args[0] = classes[0].cast(retObject);
		for (int i = 1; i < args.length; i++) {
			if (expList.size() >= i)
				args[i] = objectMapper.readValue(expList.get(i - 1), classes[i]);
		}
		return args;
	}

}
