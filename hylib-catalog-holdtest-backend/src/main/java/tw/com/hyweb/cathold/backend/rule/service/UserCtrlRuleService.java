package tw.com.hyweb.cathold.backend.rule.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tw.com.hyweb.cathold.graphql.redis.service.VUserCtrlStatusService;
import tw.com.hyweb.cathold.graphql.repository.CatalogHoldRuleRepository;
import tw.com.hyweb.cathold.graphql.rule.UserCtrlRule;
import tw.com.hyweb.cathold.sqlserver.model.ReaderInfo;
import tw.com.hyweb.cathold.sqlserver.model.ReaderType;
import tw.com.hyweb.cathold.sqlserver.repository.ReaderInfoRepository;
import tw.com.hyweb.cathold.sqlserver.repository.ReaderTypeRepository;

@Component
public class UserCtrlRuleService {

	@Value("${cathold.useritemcheck.itemUserCheckRule}")
	private String itemUserCheckRule;

	@Autowired
	private CatalogHoldRuleRepository catalogHoldRuleRepository;

	@Autowired
	private ReaderInfoRepository readerInfoRepository;

	@Autowired
	private ReaderTypeRepository readerTypeRepository;

	@Autowired
	private VUserCtrlStatusService vUserCtrlStatusService;

	@Autowired
	private ObjectMapper objectMapper;

	@PostConstruct
	public void initialRules() {
		UserCtrlRule
				.putUserRuleMap(this.catalogHoldRuleRepository.findByRuleClassNameStartingWith(this.itemUserCheckRule));
		UserCtrlRule.getUserRuleMap().values().forEach(this::convertRule);
		UserCtrlRule.setUserCtrlRuleService(this);
	}

	public void convertRule(UserCtrlRule rule) {
		String[] args = rule.getExpList();
		try {
			Class<?>[] classes = objectMapper.readerForArrayOf(Class.class).readValue(args[1]);
			rule.getCheckRi().setParameterTypes(classes);
			Object[] objs = new Object[classes.length];
			for (int i = 1; i < classes.length; i++) {
				Class<?> clazz = classes[i];
				objs[i] = objectMapper.readValue(args[i + 1], clazz);
			}
			rule.getCheckRi().setArguments(objs);
			rule.setRetBoolean(objectMapper.readValue(args[classes.length + 1], boolean.class));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	public String getUserType(int readerId) {
		try {
			ReaderInfo readerInfo = this.readerInfoRepository.findByReaderId(readerId).orElseThrow();
			ReaderType readerType = this.readerTypeRepository.findByReaderTypeId(readerInfo.getReadertypeId())
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

	public boolean userTypeIn(String readerType, Set<String> types) {
		return types.contains(readerType);
	}

	public boolean processCheck(int userRule, int readerId) {
		int i = 0;
		boolean b = userRule > 0;
		while (userRule > 0 && b) {
			int ruleNum = userRule & 15;
			if (ruleNum > 0)
				b &= this.vUserCtrlStatusService.checkUserRule(readerId, ruleNum << (i << 2));
			userRule >>= 4;
			i++;
		}
		return b;
	}

}
