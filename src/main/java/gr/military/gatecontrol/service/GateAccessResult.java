package gr.military.gatecontrol.service;

import gr.military.gatecontrol.entity.Person;

public class GateAccessResult {

    private final boolean granted;
    private final Person person;
    private final String movementType;

    public GateAccessResult(boolean granted, Person person, String movementType) {
        this.granted = granted;
        this.person = person;
        this.movementType = movementType;
    }

    public boolean isGranted() {
        return granted;
    }

    public Person getPerson() {
        return person;
    }

    public String getMovementType() {
        return movementType;
    }

    public String getPersonName() {
        if (person == null) return null;
        return person.getFirstName() + " " + person.getLastName();
    }
}