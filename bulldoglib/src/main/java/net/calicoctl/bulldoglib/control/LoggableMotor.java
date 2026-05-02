package net.calicoctl.bulldoglib.control;

import java.util.Objects;

import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.inputs.LoggableInputs;

public class LoggableMotor {

    private final String name;
    private final int id;
    private LoggableInputs inputs;

    protected LoggableMotor(String name, int id) {
        if (id < 0 || id > 62) throw new IllegalArgumentException("CAN ID of a LoggableMotor must be between 0 and 62, inclusive!");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name of a LoggableMotor must not be null, empty, or contain only whitespace characters!");

        this.name = name;
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public int getID() {
        return this.id;
    }

    protected void setInputs(LoggableInputs inputs) {
        if (inputs == null) this.inputs = Objects.requireNonNull(inputs, "Cannot give a LoggableMotor null inputs!");
    }

    protected void update() {
        if (this.inputs == null) throw new NullPointerException("Cannot update inputs of a LoggableMotor if it has not been given inputs yet");
        Logger.processInputs("Motors/" + name, inputs);
    }
    
}
