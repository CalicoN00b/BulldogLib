package net.calicoctl.bulldoglib.control;

import java.util.HashMap;

/**
 * Class for managing every Bulldog device.
 * To manage a Bulldog device, it must first be registered into the device manager,
 * and then all registered devices can be updating using {@link #updateRegisteredDevices}
 */
public class BulldogDeviceManager {

    // All CAN IDs must go to unique devices with unique names.
    private static HashMap<Integer, LoggableMotor> registeredMotors = new HashMap<>();
    private static HashMap<Integer, String> motorNames = new HashMap<>();

    /**
     * Registers a LoggableMotor to the device manager.
     * Registering a motor allows it to be included in {@link #updateRegisteredDevices}.
     * @param motor The motor to register with the device manager.
     */
    public static void registerMotor(LoggableMotor motor) {
        if (registeredMotors.containsKey(motor.getID())) {
            throw new IllegalArgumentException(String.format("Cannot register %s, as %s is already registered to CAN ID %d!", motor.getName(), registeredMotors.get(motor.getID()).getName(), motor.getID()));
        }
        
        if (motorNames.containsValue(motor.getName())) {
            throw new IllegalArgumentException(String.format("Cannot register %s, as another device is already registered to that name!", motor.getName()));
        }

        registeredMotors.put(motor.getID(), motor);
        motorNames.put(motor.getID(), motor.getName());
    }

    /**
     * Clears all registered motors from the device manager.
     * Does not affect any other types of registered devices.
     */
    public static void clearRegisteredMotors() {
        registeredMotors.clear();
        motorNames.clear();
    }

    /**
     * Removes a registered LoggableMotor.
     * @param key The CAN ID of the motor to remove.
     * @return The LoggableMotor associated with the CAN ID.
     *      If there is no LoggableMotor associated with the CAN ID, will return {@code null}.
     */
    public static LoggableMotor removeRegisteredMotor(int key) {
        motorNames.remove(key);
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