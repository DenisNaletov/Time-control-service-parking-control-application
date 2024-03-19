package parking.service.timeController.dto;

import java.time.LocalDateTime;


public record DateValue(long carRegNumber, long parkingId, LocalDateTime dateValue) {

}
