package parking.service.timeController;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import parking.service.timeController.dto.DateValue;
import parking.service.timeController.entities.ListDateValues;
import parking.service.timeController.repository.ListDateValuesRepo;

@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
public class TimeControllerTest {
	

	private static final long REGNUMBER_NEW_DATE = 123;
	
	private static final long REGNUMBER_DATE_VALUE_LESS = 456;

	private static final long REGNUMBER_DATE_VALUE_GRATER = 789;

	private static final long PARKING_ID_VALUE = 111;

	private static final LocalDateTime DATE_VALUE = LocalDateTime.of(2024, 1, 5, 10, 00, 00);


	@Autowired
	InputDestination producer;
	
	@Autowired
	OutputDestination consumer;
	
	@MockBean
	ListDateValuesRepo repo;
	
	// 3 dates for 3 scens
	
	// Date for new regNumber
	DateValue dateNoValues = new DateValue(REGNUMBER_NEW_DATE, PARKING_ID_VALUE, DATE_VALUE);
	// Date for regNumber with date less then 10 min
	DateValue dateLessValue = new DateValue(REGNUMBER_DATE_VALUE_LESS, PARKING_ID_VALUE, DATE_VALUE);
	// Date for regNumber with date grater then 10 min
	DateValue dateGraterValue = new DateValue(REGNUMBER_DATE_VALUE_GRATER, PARKING_ID_VALUE, DATE_VALUE);

	private String consumerBindingName = "ParkingEventFirstTimeControlConsumer-in-0";
	private String producerBindingName = "ParkingEventFirstTimeControlProducer-out-0";
	
	// Entites for Redis
	static ListDateValues listDateValuesLess = new ListDateValues(REGNUMBER_DATE_VALUE_LESS);
	static ListDateValues listDateValuesGrater = new ListDateValues(REGNUMBER_DATE_VALUE_GRATER);
	static List<LocalDateTime> datesValueLess;
	static List<LocalDateTime> datesValueGrater;
	
	// map to mock Redis
	static HashMap<Long, ListDateValues> redisMap = new HashMap<>();
	
	@BeforeAll
	static void setupAll() {
		datesValueLess = listDateValuesLess.getValues();
		datesValueGrater = listDateValuesGrater.getValues();
		datesValueGrater.add(DATE_VALUE);
		redisMap.put(REGNUMBER_DATE_VALUE_LESS, listDateValuesLess);
		redisMap.put(REGNUMBER_DATE_VALUE_GRATER, listDateValuesGrater);
	}
	
	@Test
	void dateNewValueTest() {
		
		when(repo.findById(REGNUMBER_NEW_DATE))
		.thenReturn(Optional.ofNullable(null));
		
		when(repo.save(new ListDateValues(REGNUMBER_NEW_DATE)))
		.thenAnswer(new Answer<ListDateValues>() {

			@Override
			public ListDateValues answer(InvocationOnMock invocation) throws Throwable {
				redisMap.put(REGNUMBER_NEW_DATE, invocation.getArgument(0));
				return invocation.getArgument(0);
			}
		});
		
		producer.send(new GenericMessage<DateValue>(dateNoValues), consumerBindingName);
		Message<byte[] > message = consumer.receive(0, producerBindingName); 
		assertNull(message);
		assertEquals(DATE_VALUE, redisMap.get(REGNUMBER_NEW_DATE).getValues().get(0));
	}
	
	@Test
	void dateValueLessTest() {
		
		when(repo.findById(REGNUMBER_DATE_VALUE_LESS))
		.thenReturn(Optional.of(listDateValuesLess));
		
		when(repo.save(new ListDateValues(REGNUMBER_DATE_VALUE_LESS)))
		.thenAnswer(new Answer<ListDateValues>() {

			@Override
			public ListDateValues answer(InvocationOnMock invocation) throws Throwable {
				redisMap.put(REGNUMBER_DATE_VALUE_LESS, invocation.getArgument(0));
				return invocation.getArgument(0);
			}
		});
		
		producer.send(new GenericMessage<DateValue>(dateLessValue), consumerBindingName);
		Message<byte[] > message = consumer.receive(0, producerBindingName); 
		assertNull(message);
		assertEquals(DATE_VALUE, redisMap.get(REGNUMBER_DATE_VALUE_LESS).getValues().get(0));
	}
	
	@Test
	void dateValueGraterTest() throws StreamReadException, DatabindException, IOException {
		
		when(repo.findById(REGNUMBER_DATE_VALUE_GRATER))
		.thenReturn(Optional.of(listDateValuesGrater));
		
		when(repo.save(new ListDateValues(REGNUMBER_DATE_VALUE_GRATER)))
		.thenAnswer(new Answer<ListDateValues>() {

			@Override
			public ListDateValues answer(InvocationOnMock invocation) throws Throwable {
				redisMap.put(REGNUMBER_DATE_VALUE_GRATER, invocation.getArgument(0));
				return invocation.getArgument(0);
			}
		});
		
		producer.send(new GenericMessage<DateValue>(dateGraterValue), consumerBindingName);
		Message<byte[] > message = consumer.receive(0, producerBindingName); 
		assertNotNull(message);
		ObjectMapper mapper = new ObjectMapper();
		assertEquals(dateGraterValue, mapper.readValue(message.getPayload(), DateValue.class));
		assertTrue(redisMap.get(REGNUMBER_DATE_VALUE_GRATER).getValues().isEmpty());
	}

}
