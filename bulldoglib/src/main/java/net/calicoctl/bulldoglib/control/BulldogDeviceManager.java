package net.calicoctl.bulldoglib.control;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class BulldogDeviceManager {

    private static HashSet<Integer> usedIDs = new HashSet<Integer>();

    private static List<BulldogTalonFX> talonFXs = new LinkedList<BulldogTalonFX>();
    private static List<BulldogSparkFlex> sparkFlexs = new LinkedList<BulldogSparkFlex>();

    public static void registerMotor(BulldogTalonFX talonFX) {
        if (!usedIDs.add(talonFX.motor.getDeviceID())) throw new IllegalArgumentException("CAN ID " + talonFX.motor.getDeviceID() + " is in use by another device!");
        talonFXs.add(talonFX);
    }

    public static void registerMotor(BulldogSparkFlex sparkFlex) {
        if (!usedIDs.add(sparkFlex.motor.getDeviceId())) throw new IllegalArgumentException("CAN ID " + sparkFlex.motor.getDeviceId() + " is in use by another device!");
        sparkFlexs.add(sparkFlex);
    }

    public static void updateAllDevices() {
        for (BulldogTalonFX talonFX : talonFXs) {
            talonFX.update();
        }

        for (BulldogSparkFlex sparkFlex : sparkFlexs) {
            sparkFlex.update();
        }
    }
}