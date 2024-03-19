package parking.service.timeController;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;

import parking.service.timeController.dto.DateValue;
import parking.service.timeController.service.IDateController;

@SpringBootApplication
public class TimeControllerAppl {
	
	@Autowired
	IDateController service;
	
	@Autowired
	StreamBridge bridge;
	
	@Value("${app.time.controller.producer.binding.name:ParkingEventFirstTimeControlProducer-out-0}")
	String bindingName;

	public static void main(String[] args) {
		SpringApplication.run(TimeControllerAppl.class, args);
	}
	
	@Bean
	Consumer<DateValue> dateController(){
		return (dateValue) -> {
			Boolean res = service.dateController(dateValue);
			if(res != null) {
				DateValue date = new DateValue(dateValue.carRegNumber(), dateValue.parkingId(), dateValue.dateValue());
				bridge.send(bindingName, date);
			}
				
		};
	}

}
