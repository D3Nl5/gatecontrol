//package gr.military.gatecontrol;
//
//import gr.military.gatecontrol.entity.Movement;
//import gr.military.gatecontrol.entity.Person;
//import gr.military.gatecontrol.repository.PersonRepository;
//import gr.military.gatecontrol.service.MovementService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//
//@SpringBootTest
//public class MovementServiceTest {
//
//    @Autowired
//    private MovementService movementService;
//
//    @Autowired
//    private PersonRepository personRepository;
//
//    @Test
//    void testSaveAndRetrieve() {
//        // 1. create a Person
//        Person p = new Person();
//        p.setFirstName("John");
//        p.setLastName("Doe");
//        p.setAsm("123456");
//        p.setRfidUid("ABCDEF123");
//        p.setActive(true);
//
//        personRepository.save(p);
//
//        // 2. create a Movement linked to that Person
//        Movement m = new Movement();
//        m.setGateName("Main Gate");
//        m.setMovementType("IN");
//        m.setMovementTime(LocalDateTime.now());
//        m.setOperator("Operator1");
//        m.setPerson(p);  // <-- this is required
//
//        movementService.logMovement(m);
//
//        // 3. assert it is saved
//        List<Movement> all = movementService.getAll();
//        assertFalse(all.isEmpty());
//        assertEquals(p.getId(), all.get(0).getPerson().getId()); // check linkage
//    }
//}