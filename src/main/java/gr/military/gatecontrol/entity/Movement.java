package gr.military.gatecontrol.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Movement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "movement_type", nullable = false)
    private String movementType; // "ΕΝΤΟΣ" or "ΕΚΤΟΣ"

    @Column(name = "movement_time")
    private LocalDateTime movementTime = LocalDateTime.now();

    @Column(name = "gate_name")
    private String gateName = "MainGate";

    @Column(name = "operator")
    private String operator;
}