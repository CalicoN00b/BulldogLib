package net.calicoctl.bulldoglib.control;

import java.util.HashMap;

/**
 * Class for managing every Bulldog device.
 * To manage a Bulldog device, it must first be registered into the device manager,
 * and then all registered devices can be updating using {@link #updateRegisteredDevices}
 */
public class BulldogDeviceManager {

    private static HashMap<Integer, LoggableMotor> registeredMotors = new HashMap<>();

    /**
     * Registers a LoggableMotor to the device manager.
     * Registering a motor allows it to be included in {@link #updateRegisteredDevices}.
     * @param motor The motor to register with the device manager.
     */
    public static void registerMotor(LoggableMotor motor) {
        if (registeredMotors.containsKey(motor.getID())) {
            throw new IllegalArgumentException(String.format("Cannot register %s, as %s is already registered to CAN ID %d!", motor.getName(), registeredMotors.get(motor.getID()).getName(), motor.getID()));
        }
        registeredMotors.put(motor.getID(), motor);
    }

    /**
     * Clears all registered motors from the device manager.
     * Does not affect any other types of registered devices.
     */
    public static void clearRegisteredMotors() {
        registeredMotors.clear();
    }

    /**
     * Removes a registered LoggableMotor.
     * @param key The CAN ID of the motor to remove.
     * @return The LoggableMotor associated with the CAN ID.
     *      If there is no LoggableMotor associated with the CAN ID, will return {@code null}.
     */
    public static LoggableMotor removeRegisteredMotor(int key) {
        return registeredMotors.remove(key);
    }

    /**
     * Updates all registered devices.
     * @see LoggableMotor#update
     * @see BulldogTalonFX#update
     * @see BulldogSparkFlex#update
     */
    public static void updateRegisteredDevices() {
        for (LoggableMotor motor : registeredMotors.values()) {
            motor.update();
        }
    }

}