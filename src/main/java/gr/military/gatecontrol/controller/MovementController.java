package gr.military.gatecontrol.controller;

import gr.military.gatecontrol.entity.Movement;
import gr.military.gatecontrol.entity.Person;
import gr.military.gatecontrol.service.MovementService;
import gr.military.gatecontrol.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movement")
@RequiredArgsConstructor
public class MovementController {

    private final MovementService movementService;
    private final PersonService personService;

    @PostMapping("/log")
    public Movement logMovement(@RequestParam String rfidUid, @RequestParam String movementType) {
        Person person = personService.getByRfid(rfidUid)
                .orElseThrow(() -> new RuntimeException("Person not found with RFID: " + rfidUid));

        Movement movement = Movement.builder()
                .person(person)
                .movementType(movementType)
                .build();

        return movementService.logMovement(movement);
    }

    @GetMapping("/all")
    public List<Movement> getAll() {
        return movementService.getAll();
    }

    @GetMapping("/person/{personId}")
    public List<Movement> getByPerson(@PathVariable Long personId) {
        Person person = personService.getById(personId)
                .orElseThrow(() -> new RuntimeException("Person not found with id: " + personId));

        return movementService.getByPerson(person);
    }
}