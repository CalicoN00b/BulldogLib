package net.calicoctl.bulldoglib.control;

import java.util.Objects;

import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.inputs.LoggableInputs;

/**
 * Template class for a LoggableMotor.
 * Cannot be directly instantiated, so any class wishing to use LoggableMotor must extend it.
 */
public class LoggableMotor {

    private final String name;
    private final int id;
    private LoggableInputs inputs;

    /**
     * Creates and registers a new LoggableMotor, with the given name and id.
     * @param name The name to give to the LoggableMotor.
     * @param id The CAN ID to give to the LoggableMotor
     * @throws IllegalArgumentException CAN ID is not between 0 and 62, inclusive.
     * @throws IllegalArgumentException Name is null, empty, or contains only whitespace characters.
     * @throws IllegalArgumentException A LoggableMotor has already been registered with the desired CAN ID.
     */
    protected LoggableMotor(String name, int id) {
        if (id < 0 || id > 62) throw new IllegalArgumentException("CAN ID of a LoggableMotor must be between 0 and 62, inclusive!");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name of a LoggableMotor must not be null, empty, or contain only whitespace characters!");

        this.name = name;
        this.id = id;

        BulldogDeviceManager.registerMotor(this);
    }

    /**
     * Returns the name of the LoggableMotor.
     * @return The name of the LoggableMotor.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the CAN ID of the LoggableMotor.
     * @return the CAN ID of the LoggableMotor.
     */
    public int getID() {
        return this.id;
    }

    /**
     * Sets the inputs of the LoggableMotor. MUST be called before calling {@link #update}.
     * @param inputs
     */
    protected void setInputs(LoggableInputs inputs) {
        if (inputs == null) this.inputs = Objects.requireNonNull(inputs, "Cannot give a LoggableMotor null inputs!");
    }

    /**
     * Processes the inputs of the LoggableMotor. Cannot process inputs until {@link #setInputs} has been called.
     * @throws NullPointerException Inputs have not been set.
     */
    protected void update() {
        if (this.inputs == null) throw new NullPointerException("Cannot update inputs of a LoggableMotor if it has not been given inputs yet");
        Logger.processInputs("Motors/" + name, inputs);
    }
    
}
