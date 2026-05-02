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

public class BulldogSparkFlex extends LoggableMotor {

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

    public BulldogSparkFlex(int id) {
        this(id, "Motor" + id, new SparkFlexConfig(), MotorType.kBrushless, false, false);
    }

    public BulldogSparkFlex(int id, MotorType motorType) {
        this(id, "Motor" + id, new SparkFlexConfig(), motorType, false, false);
    }

    public BulldogSparkFlex(int id, String name) {
        this(id, name, new SparkFlexConfig(), MotorType.kBrushless, false, false);
    }

    public BulldogSparkFlex(int id, String name, MotorType motorType) {
        this(id, name, new SparkFlexConfig(), motorType, false, false);
    }

    public BulldogSparkFlex(int id, String name, SparkFlexConfig config) {
        this(id, name, config, MotorType.kBrushless, false, false);
    }

    public BulldogSparkFlex(int id, String name, SparkFlexConfig config, MotorType motorType) {
        this(id, name, config, motorType, false, false);
    }

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

    public BulldogSparkFlex withLeader(int leaderID, boolean opposeLeader) {
        config.follow(leaderID, opposeLeader);
        return this;
    }

    public BulldogSparkFlex withLeader(BulldogSparkFlex leaderMotor, boolean opposeLeader) {
        return this.withLeader(leaderMotor.motor.getDeviceId(), opposeLeader);
    }

    public BulldogSparkFlex withBrakeMode(boolean value) {
        config.idleMode(value ? IdleMode.kBrake : IdleMode.kCoast);
        return this;
    }

    public BulldogSparkFlex withInverted(boolean value) {
        config.inverted(value);
        return this;
    }

    public void set(double output) {
        if (output < -1) output = -1;
        else if (output > 1) output = 1;
        motor.set(output);
    }

    public void stop() {
        motor.stopMotor();
    }

    public void resetPosition(double position) {
        if (relativeEncoder != null) {
            relativeEncoder.setPosition(position);
        }
    }

    public double getAppliedVoltage() {
        return loggedAppliedVoltage;
    }

    public double getSupplyCurrent() {
        return loggedSupplyCurrent;
    }

    public double getPosition() {
        return loggedPosition;
    }

    public double getVelocity() {
        return loggedVelocity;
    }

    public double getTempurature() {
        return loggedTemperature;
    }

    public boolean hasActiveFault() {
        return loggedHasActiveFault;
    }
    
}
