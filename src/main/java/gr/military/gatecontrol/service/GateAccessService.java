package gr.military.gatecontrol.service;

import gr.military.gatecontrol.entity.Movement;
import gr.military.gatecontrol.entity.Person;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GateAccessService {

    private final PersonService personService;
    private final MovementService movementService;

    public GateAccessResult handleCardRead(String uid) {
        Optional<Person> personOpt = personService.getByRfid(uid);
        if (personOpt.isEmpty()) {
            return new GateAccessResult(false, null, null);
        }

        Person person = personOpt.get();
        if (!person.isActive()) {
            return new GateAccessResult(false, null, null);
        }

        String movementType = determineNextMovement(person);

        Movement movement = new Movement();
        movement.setPerson(person);
        movement.setGateName(gr.military.gatecontrol.config.AppConfig.getGateName());
        movement.setMovementTime(LocalDateTime.now());
        movement.setMovementType(movementType);
        movement.setOperator("System");

        movementService.logMovement(movement);

        return new GateAccessResult(true, person, movementType);
    }

    private String determineNextMovement(Person person) {
        Movement last = movementService.getLastMovement(person);
        if (last == null) return "ΕΝΤΟΣ";
        return "ΕΝΤΟΣ".equals(last.getMovementType()) ? "ΕΚΤΟΣ" : "ΕΝΤΟΣ";
    }
}