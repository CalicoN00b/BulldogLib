package net.calicoctl.bulldoglib.control;

import java.util.Objects;

import org.littletonrobotics.junction.LogTable;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.inputs.LoggableInputs;

import com.revrobotics.AbsoluteEncoder;
import com.revrobotics.PersistMode;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.ResetMode;
import com.revrobotics.spark.SparkFlex;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.config.ClosedLoopConfigAccessor;
import com.revrobotics.spark.config.FeedForwardConfigAccessor;
import com.revrobotics.spark.config.SparkFlexConfig;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;

import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.math.filter.Debouncer.DebounceType;
import edu.wpi.first.wpilibj.Alert;
import edu.wpi.first.wpilibj.Alert.AlertType;
import net.calicoctl.bulldoglib.util.BulldogTunableNumber;

/**
 * A wrapper class for {@link SparkFlex}.
 * Uses the <a href="https://github.com/Mechanical-Advantage/AdvantageKit">AdvantageKit</a> library to log many aspects of the motor,
 * and will automatically alert the user if a motor has active faults.
 * <p>
 * If tuning is enabled for this motor, allows tuning of PID and Feedforward.
 */
public class BulldogSparkFlex extends LoggableMotor {

    /**
     * The backing motor of the BulldogSparkFlex.
     * Should only be used for scenarios that BulldogLib has not accounted for.
     */
    public final SparkFlex motor;
    private final SparkFlexConfig config;

    private final AbsoluteEncoder absoluteEncoder;
    private final RelativeEncoder relativeEncoder;

    private double loggedAppliedVoltage;
    private double loggedSupplyCurrent;
    private double loggedPosition;
    private double loggedVelocity;
    private double loggedTemperature;
    private boolean loggedHasActiveFault;

    private final BulldogTunableNumber kP;
    private final BulldogTunableNumber kI;
    private final BulldogTunableNumber kD;
    private final BulldogTunableNumber kS;
    private final BulldogTunableNumber kV;
    private final BulldogTunableNumber kA;
    private final BulldogTunableNumber kG;

    private final Alert activeFaultAlert;
    private final Debouncer alertDebouncer = new Debouncer(0.5, DebounceType.kFalling);

    /**
     * Creates a new BulldogSparkFlex with the given id, the given name, and the given config.
     * The backing motor's type will follow the given motor type and will use an absolute encoder if enabled.
     * @param id The CAN ID of the motor.
     * @param name The name of the motor.
     * @param config The configs to give to the motor.
     * @param motorType The motor type of the motor.
     * @param useAbsoluteEncoder Whether or not to use an absolute encoder.
     *      If true, will use an absolute encoder.
     *      If false, will use a relative encoder.
     * @param enableTuning Whether or not to enable tuning of the motor.
     *      If true, will enable tuning of PID and Feedforward values.
     *      If false, will default to the PID and Feedforward values specified by the given configs.
     */
    public BulldogSparkFlex(int id, String name, SparkFlexConfig config, MotorType motorType, boolean useAbsoluteEncoder, boolean enableTuning) {
        super(name, id);
        super.setInputs(
            new LoggableInputs() {
            public void toLog(LogTable table) {
                table.put("AppliedVoltage", loggedAppliedVoltage);
                table.put("SupplyCurrent", loggedSupplyCurrent);
                table.put("Position", loggedPosition);
                table.put("Velocity", loggedVelocity);
                table.put("Tempurature", loggedTemperature);
                table.put("HasActiveFault", loggedHasActiveFault);
            }

            public void fromLog(LogTable table) {
                loggedAppliedVoltage = table.get("AppliedVoltage", 0);
                loggedSupplyCurrent = table.get("SupplyCurrent", 0);
                loggedPosition = table.get("Position", 0);
                loggedVelocity = table.get("Velocity", 0);
                loggedTemperature = table.get("Tempurature", 0);
                loggedHasActiveFault = table.get("HasActiveFault", false);
            }
        }
        );

        this.config = Objects.requireNonNull(config, "Config must not be null!");

        motor = new SparkFlex(id, Objects.requireNonNull(motorType, "Motor type must not be null!"));
        motor.configure(config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

        if (useAbsoluteEncoder) {
            absoluteEncoder = motor.getAbsoluteEncoder();
            relativeEncoder = null;
        } else {
            absoluteEncoder = null;
            relativeEncoder = motor.getEncoder();
        }

        ClosedLoopConfigAccessor closedLoopConfigAccessor = motor.configAccessor.closedLoop;
        FeedForwardConfigAccessor feedForwardConfigAccessor = motor.configAccessor.closedLoop.feedForward;
        kP = new BulldogTunableNumber(name + "/kP", closedLoopConfigAccessor.getP(), enableTuning);
        kI = new BulldogTunableNumber(name + "/kI", closedLoopConfigAccessor.getI(), enableTuning);
        kD = new BulldogTunableNumber(name + "/kD", closedLoopConfigAccessor.getD(), enableTuning);
        kS = new BulldogTunableNumber(name + "/kS", feedForwardConfigAccessor.getkS(), enableTuning);
        kV = new BulldogTunableNumber(name + "/kV", feedForwardConfigAccessor.getkV(), enableTuning);
        kA = new BulldogTunableNumber(name + "/kA", feedForwardConfigAccessor.getkA(), enableTuning);
        kG = new BulldogTunableNumber(name + "/kG", feedForwardConfigAccessor.getkG(), enableTuning);

        activeFaultAlert = new Alert(name + " has active fault(s)!", AlertType.kError);
    }

    /**
     * Updates and processes the inputs of the motor. Also will alert the user if the motor has active faults.
     * <p>
     * Called periodically (every loop) as part of {@link BulldogDeviceManager#updateRegisteredDevices}.
     */
    @Override
    protected void update() {
        if (!Logger.hasReplaySource()) {
            loggedAppliedVoltage = motor.getAppliedOutput() * motor.getBusVoltage();
            loggedSupplyCurrent = motor.getOutputCurrent();
            loggedPosition = absoluteEncoder != null ? absoluteEncoder.getPosition() : relativeEncoder.getPosition();
            loggedVelocity = absoluteEncoder != null ? absoluteEncoder.getVelocity() : relativeEncoder.getVelocity();
            loggedTemperature = motor.getMotorTemperature();
            loggedHasActiveFault = motor.hasActiveFault();
        }

        super.update();

        BulldogTunableNumber.ifChanged(
            hashCode(), 
            (values) -> {
                config.closedLoop.pid(values[0], values[1], values[2]);
                config.closedLoop.feedForward.kS(values[3]);
                config.closedLoop.feedForward.kV(values[4]);
                config.closedLoop.feedForward.kA(values[5]);
                config.closedLoop.feedForward.kG(values[6]);

                motor.configure(config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
            },
            kP, kI, kD, kS, kV, kA, kG
        );

        activeFaultAlert.set(alertDebouncer.calculate(loggedHasActiveFault));
    }

    /**
     * Creates a copy of this BulldogSparkFlex with the Follower config applied to the backing motor.
     * @param leaderID The CAN ID of the motor to follow.
     * @param opposeLeader {@code true} to oppose the output of the leader, {@code false} to align to the output of the leader.
     * @return A copy of this BulldogSparkFlex, following the motor at the given ID.
     */
    public BulldogSparkFlex withLeader(int leaderID, boolean opposeLeader) {
        config.follow(leaderID, opposeLeader);
        return this;
    }

    /**
     * Creates a copy of this BulldogSparkFlex with the Follower config applied to the backing motor.
     * @param leaderMotor The BulldogSparkFlex to follow.
     * @param opposeLeader {@code true} to oppose the output of the leader, {@code false} to align to the output of the leader.
     * @return A copy of this BulldogSparkFlex, following the motor at the given ID.
     */
    public BulldogSparkFlex withLeader(BulldogSparkFlex leaderMotor, boolean opposeLeader) {
        return this.withLeader(leaderMotor.motor.getDeviceId(), opposeLeader);
    }

    /**
     * Creates a copy of this BulldogSparkFlex with the applied IdleMode config.
     * @param value {@code true} to enable brake mode, {@code false} to enable coast mode.
     * @return A copy of this BulldogSparkFlex with the desired IdleMode config.
     */
    public BulldogSparkFlex withBrakeMode(boolean value) {
        config.idleMode(value ? IdleMode.kBrake : IdleMode.kCoast);
        return this;
    }

    /**
     * Creates a copy of this BulldogSparkFlex with the desired inverted config.
     * @param value {@code true} to invert the motor, {@code false} to uninvert the motor.
     * @return A copy of this BulldogSparkFlex with the desired inverted config.
     */
    public BulldogSparkFlex withInverted(boolean value) {
        config.inverted(value);
        return this;
    }

    /**
     * Sets the speed of the motor.
     * @param output The speed to set. Should be between -1.0 and 1.0, inclusive.
     */
    public void set(double output) {
        if (output < -1) output = -1;
        else if (output > 1) output = 1;
        motor.set(output);
    }

    /**
     * Stops the motor.
     * <p>
     * If brake mode is enabled, will hold the motor still.
     * If coast mode is enabled, will not hold the motor in place, allowing it to rotate freely from external forces.
     */
    public void stop() {
        motor.stopMotor();
    }

    /**
     * Resets the position of the motor to the given position.
     * <p>
     * If using an absolute encoder, will do nothing.
     * @param position The position to reset to, in Rotations.
     */
    public void resetPosition(double position) {
        if (relativeEncoder != null) {
            relativeEncoder.setPosition(position);
        }
    }

    /**
     * Gets the applied voltage to the motor.
     * @return The applied voltage, in Volts.
     */
    public double getAppliedVoltage() {
        return loggedAppliedVoltage;
    }

    /**
     * Gets the supply current of the motor.
     * @return The supply current, in Amps.
     */
    public double getSupplyCurrent() {
        return loggedSupplyCurrent;
    }

    /**
     * Gets the position of the motor.
     * @return The position, in Rotations.
     */
    public double getPosition() {
        return loggedPosition;
    }

    /**
     * Gets the velocity of the motor.
     * @return The velocity, in Rotations per Minute.
     */
    public double getVelocity() {
        return loggedVelocity;
    }

    /**
     * Gets the tempurature of the motor.
     * @return The tempurature, in Celsius.
     */
    public double getTempurature() {
        return loggedTemperature;
    }

    /**
     * Returns whether the motor has an active fault.
     * @return {@code true} if the motor has an active fault, {@code false} otherwise.
     */
    public boolean hasActiveFault() {
        return loggedHasActiveFault;
    }
    
}
