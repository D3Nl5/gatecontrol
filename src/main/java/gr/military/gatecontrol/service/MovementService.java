package gr.military.gatecontrol.service;

import gr.military.gatecontrol.entity.Movement;
import gr.military.gatecontrol.entity.Person;
import gr.military.gatecontrol.repository.MovementRepository;
import gr.military.gatecontrol.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovementService {

    private final MovementRepository movementRepository;
    private final PersonRepository personRepository;

    public Movement logMovement(Movement movement) {
        return movementRepository.save(movement);
    }

    public List<Movement> getByPerson(Person person) {
        return movementRepository.findByPerson(person);
    }

    public List<Movement> getAll() {
        return movementRepository.findAll();
    }

    public Movement getLastMovement(Person person) {
        return movementRepository.findTopByPersonOrderByMovementTimeDesc(person);
    }

    /** Returns the single most recent movement per person (single query). */
    public List<Movement> getLastMovementsForAll() {
        return movementRepository.findLatestPerPerson();
    }

    public List<Person> getAllPersons() {
        return personRepository.findAll();
    }
}