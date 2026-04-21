package net.calicoctl.bulldoglib.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

import org.littletonrobotics.junction.networktables.LoggedNetworkNumber;

/**
 * Class for a tunable number. In tuning mode, returns the value from the dashboard.
 * If not in tuning mode or the number is not in the dashboard, returns the default value.
 * <p>
 * Based on the LoggedTunableNumber that can be found in many of the repositories of
 * <a href="https://github.com/Mechanical-Advantage">FRC Team 6328: Mechanical Advantage</a>,
 * with some slight modifications.
 * <p>
 * The main difference between this implementation of a tunable number and Mechanical Advantage's is that
 * this implementation allows enabling tuning for each number separately, whereas Mechanical Advantage's
 * only allows enabling tuning globally.
 */
public class BulldogTunableNumber implements DoubleSupplier {

    private static final String tableKey = "/Tuning/";

    private final String dashboardKey;
    private final double defaultValue;
    private final boolean enableTuning;
    private final Map<Integer, Double> lastHasChangedValue = new HashMap<>();

    private LoggedNetworkNumber dashboardNumber;

    /**
     * Creates a new BulldogTunableNumber with the given key, default value, and whether or not to enable tuning.
     * 
     * @param dashboardKey The key to give to the tunable number. If tuning is enabled,
     *      this BulldogTunableNumber will show up in the dashboard under "/Tuning/{dashboardKey}"
     * @param defaultValue The default value to give to the tunable number.
     * @param enableTuning Whether or not to enable tuning of this number.
     * @throws IllegalArgumentException if name is null, empty or contains only whitespace characters.
     */
    public BulldogTunableNumber(String dashboardKey, double defaultValue, boolean enableTuning) {
        if (dashboardKey == null || dashboardKey.isBlank()) throw new IllegalArgumentException("Cannot create a BulldogTunableNumber with a null or empty key!");

        this.dashboardKey = tableKey + dashboardKey;
        this.defaultValue = defaultValue;
        this.enableTuning = enableTuning;

        if (this.enableTuning) {
            dashboardNumber = new LoggedNetworkNumber(this.dashboardKey, this.defaultValue);
        }
    }

    /**
     * Gets the current value of this tunable number.
     * 
     * @return If tuning is enabled, returns the number from the dashboard, if available. Otherwise, returns the default value.
     */
    @Override
    public double getAsDouble() {
        return enableTuning ? dashboardNumber.getAsDouble() : defaultValue;
    }

    /**
     * Checks to see whether or not the value has changed since the last time this method was called.
     * 
     * @param id A unique identifier for a calling object to avoid conflicts if this BulldogTunableNumber is shared between multiple objects.
     *      A recommended approach is to pass in the {@code hashCode()} method of the object calling this method.
     * @return True if the number has changed since the last time this method was called, false otherwise.
     */
    public boolean hasChanged(int id) {
        double currentVal = getAsDouble();
        Double lastVal = lastHasChangedValue.get(id);
        if (lastVal == null || currentVal != lastVal) {
            lastHasChangedValue.put(id, currentVal);
            return true;
        }
        return false;
    }

    /**
     * Runs an action if any of the provided BulldogTunableNumbers have changed.
     * 
     * @param id A unique identifier for a calling object to avoid conflicts if the given BulldogTunableNumbers are shared between multiple objects.
     *      A recommended approach is to pass in the {@code hashCode()} method of the object calling this method.
     * @param action The action to run if any of the numbers have changed. Passes the current value of the given tunable numbers
     *      to the consumer in the order that they were input into this method.
     * @param tunableNumbers The tunable numbers to check.
     */
    public static void ifChanged(int id, Consumer<double[]> action, BulldogTunableNumber... tunableNumbers) {
        if (Arrays.stream(tunableNumbers).anyMatch(tunableNumber -> tunableNumber.hasChanged(id))) {
            action.accept(Arrays.stream(tunableNumbers).mapToDouble(BulldogTunableNumber::getAsDouble).toArray());
        }
    }

    /**
     * Runs an action if any of the provided BulldogTunableNumbers have changed.
     * 
     * @param id A unique identifier for a calling object to avoid conflicts if the given BulldogTunableNumbers are shared between multiple objects.
     *      A recommended approach is to pass in the {@code hashCode()} method of the object calling this method.
     * @param action The action to run if any of the numbers have changed.
     * @param tunableNumbers The tunable numbers to check.
     */
    public static void ifChanged(int id, Runnable action, BulldogTunableNumber... tunableNumbers) {
        ifChanged(id, values -> action.run(), tunableNumbers);
    }

}
