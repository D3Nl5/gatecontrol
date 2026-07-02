package gr.military.gatecontrol.config;

import gr.military.gatecontrol.entity.Movement;
import gr.military.gatecontrol.entity.Person;
import gr.military.gatecontrol.repository.MovementRepository;
import gr.military.gatecontrol.repository.PersonRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("h2")
public class H2DataInitializer implements ApplicationRunner {

    private static final String IN  = "\u0395\u039D\u03A4\u039F\u03A3"; // ENTOS
    private static final String OUT = "\u0395\u039A\u03A4\u039F\u03A3"; // EKTOS

    private final PersonRepository  personRepo;
    private final MovementRepository movementRepo;

    public H2DataInitializer(PersonRepository personRepo, MovementRepository movementRepo) {
        this.personRepo  = personRepo;
        this.movementRepo = movementRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (personRepo.count() > 0) return; // already seeded

        Person p1  = save("James",   "Mitchell",  "SGT", "A1B2C3D4", "ABC-1234", "Car",        "Blue");
        Person p2  = save("Robert",  "Harris",    "CPL", "B2C3D4E5", "XYZ-5678", "Jeep / SUV", "Black");
        Person p3  = save("Michael", "Thompson",  "PVT", "C3D4E5F6", null,        null,         null);
        Person p4  = save("David",   "Anderson",  "LT",  "D4E5F6A7", "LTA-0001", "Car",        "White");
        Person p5  = save("William", "Clark",     "CPT", "E5F6A7B8", "CPT-1111", "Car",        "Silver");
        Person p6  = save("Richard", "Lewis",     "SGT", "F6A7B8C9", null,        null,         null);
        Person p7  = save("Thomas",  "Walker",    "PVT", "A7B8C9D0", null,        null,         null);
        Person p8  = save("Charles", "Hall",      "CPL", "B8C9D0E1", "HAL-3344", "Motorcycle", "Red");
        Person p9  = save("Joseph",  "Young",     "MAJ", "C9D0E1F2", "MAJ-2024", "Car",        "Black");
        Person p10 = saveInactive("Daniel", "King", "PVT", "D0E1F2A3");

        LocalDateTime d = LocalDateTime.now().minusDays(1);

        // Mitchell — IN, OUT, IN  → currently INSIDE
        move(p1, IN,  d.withHour(7).withMinute(45));
        move(p1, OUT, d.withHour(12).withMinute(10));
        move(p1, IN,  d.withHour(13).withMinute(5));

        // Harris — IN, OUT → currently OUTSIDE
        move(p2, IN,  d.withHour(8).withMinute(0));
        move(p2, OUT, d.withHour(17).withMinute(0));

        // Thompson — IN → currently INSIDE
        move(p3, IN,  d.withHour(6).withMinute(30));

        // Anderson — IN, OUT → currently OUTSIDE
        move(p4, IN,  d.minusDays(1).withHour(8).withMinute(15));
        move(p4, OUT, d.minusDays(1).withHour(14).withMinute(30));

        // Clark — IN → currently INSIDE
        move(p5, IN,  d.withHour(7).withMinute(55));

        // Lewis — IN, OUT → currently OUTSIDE
        move(p6, IN,  d.withHour(8).withMinute(45));
        move(p6, OUT, d.withHour(16).withMinute(0));

        // Walker — IN, OUT → currently OUTSIDE
        move(p7, IN,  d.minusDays(2).withHour(9).withMinute(0));
        move(p7, OUT, d.minusDays(2).withHour(18).withMinute(0));

        // Hall — IN → currently INSIDE
        move(p8, IN,  d.withHour(8).withMinute(20));

        // Young — IN, OUT → currently OUTSIDE
        move(p9, IN,  d.withHour(7).withMinute(30));
        move(p9, OUT, d.withHour(9).withMinute(0));

        // King (inactive) — no movements
    }

    private Person save(String first, String last, String rank, String rfid,
                        String plate, String type, String color) {
        return personRepo.save(Person.builder()
                .firstName(first).lastName(last).mrank(rank).rfidUid(rfid)
                .active(true)
                .vehicle1Plate(plate).vehicle1Type(type).vehicle1Color(color)
                .build());
    }

    private Person saveInactive(String first, String last, String rank, String rfid) {
        return personRepo.save(Person.builder()
                .firstName(first).lastName(last).mrank(rank).rfidUid(rfid)
                .active(false)
                .build());
    }

    private void move(Person person, String type, LocalDateTime time) {
        movementRepo.save(Movement.builder()
                .person(person)
                .movementType(type)
                .movementTime(time)
                .gateName("Main Gate")
                .operator("System")
                .build());
    }
}
