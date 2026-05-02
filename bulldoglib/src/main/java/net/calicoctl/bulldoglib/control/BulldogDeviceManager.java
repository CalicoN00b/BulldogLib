package net.calicoctl.bulldoglib.control;

import java.util.HashMap;

public class BulldogDeviceManager {

    private static HashMap<Integer, LoggableMotor> registeredMotors = new HashMap<>();

    public static void registerMotor(LoggableMotor motor) {
        if (registeredMotors.containsKey(motor.getID())) {
            throw new IllegalArgumentException(String.format("Cannot register %s, as %s is already registered to CAN ID $d!", motor.getName(), registeredMotors.get(motor.getID()), motor.getID()));
        }
        registeredMotors.put(motor.getID(), motor);
    }

    public static void updateAllDevices() {
        for (LoggableMotor motor : registeredMotors.values()) {
            motor.update();
        }
    }

}