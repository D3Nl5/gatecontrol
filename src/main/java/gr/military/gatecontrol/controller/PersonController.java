package gr.military.gatecontrol.controller;

import gr.military.gatecontrol.entity.Person;
import gr.military.gatecontrol.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/person")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @GetMapping
    public List<Person> getAll() {
        return personService.getAll();
    }

    @GetMapping("/{id}")
    public Optional<Person> getById(@PathVariable Long id) {
        return personService.getById(id);
    }

    @PostMapping
    public Person create(@RequestBody Person person) {
        return personService.save(person);
    }
}