package gr.military.gatecontrol.service;

import gr.military.gatecontrol.entity.Person;
import gr.military.gatecontrol.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    public List<Person> getAll() {
        return personRepository.findAll();
    }

    public Optional<Person> getById(Long id) {
        return personRepository.findById(id);
    }

    public Optional<Person> getByRfid(String rfidUid) {
        return personRepository.findByRfidUid(rfidUid);
    }

    public Person save(Person person) {
        return personRepository.save(person);
    }

    public void delete(Person person) {
        personRepository.delete(person);
    }

    public void deleteById(Long id) {
        personRepository.deleteById(id);
    }
}