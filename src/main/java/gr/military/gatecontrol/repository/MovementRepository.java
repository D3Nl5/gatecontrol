package gr.military.gatecontrol.repository;

import gr.military.gatecontrol.entity.Movement;
import gr.military.gatecontrol.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovementRepository extends JpaRepository<Movement, Long> {

    List<Movement> findByPerson(Person person);

    Movement findTopByPersonOrderByMovementTimeDesc(Person person);

    /** Single query: latest movement per person (by max id, which is insertion order). */
    @Query("SELECT m FROM Movement m WHERE m.id IN " +
           "(SELECT MAX(m2.id) FROM Movement m2 GROUP BY m2.person.id)")
    List<Movement> findLatestPerPerson();
}