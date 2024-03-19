package parking.service.timeController.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import parking.service.timeController.dto.DateValue;
import parking.service.timeController.entities.ListDateValues;
import parking.service.timeController.repository.ListDateValuesRepo;

@Service
@Slf4j
public class DateControllerImpl implements IDateController {

	@Autowired
	ListDateValuesRepo repo;
	
	@Value("${app.comparing.size:10}")
	int comparingSize;
	
	@Override
	public Boolean dateController(DateValue date) {
		long regNumber = date.carRegNumber();
		ListDateValues list = repo.findById(regNumber).orElse(null);
		if(list== null) {
			log.debug("Parking date&time for regNumber {} wasn't find in Redis", regNumber);
			list = new ListDateValues(regNumber);
		}
		List<LocalDateTime> values = list.getValues();
		values.add(date.dateValue());
		
		if((date.dateValue().getMinute() - LocalDateTime.now().getMinute()) > comparingSize) {
			log.debug("Parking date&time for regNumber {} greater then 10 minutes", regNumber);
			values.clear();
			repo.save(list);
			return true;
		}
		else {
			log.trace("Parking date&time for regNumber {} less then 10 minutes", regNumber);
			repo.save(list);
			return false;
		}
		
	}

}
