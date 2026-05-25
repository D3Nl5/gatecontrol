package gr.military.gatecontrol.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Person")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "rfid_uid")
    private String rfidUid;

    @Lob
    @Column(name = "photo")
    private byte[] photo;

    @Column(name = "category")
    private String category;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "mrank")
    private String mrank;

    @Column(name = "vehicle1_plate")
    private String vehicle1Plate;

    @Column(name = "vehicle1_type")
    private String vehicle1Type;

    @Column(name = "vehicle1_color")
    private String vehicle1Color;

    @Column(name = "vehicle2_plate")
    private String vehicle2Plate;

    @Column(name = "vehicle2_type")
    private String vehicle2Type;

    @Column(name = "vehicle2_color")
    private String vehicle2Color;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Movement> movements;
}