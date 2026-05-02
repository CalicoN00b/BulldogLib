package net.calicoctl.bulldoglib.control;

import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.ControlRequest;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.controls.NeutralOut;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.MotorAlignmentValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.math.filter.Debouncer.DebounceType;
import edu.wpi.first.units.measure.*;
import edu.wpi.first.wpilibj.Alert;
import edu.wpi.first.wpilibj.Alert.AlertType;
import net.calicoctl.bulldoglib.util.BulldogTunableNumber;

import java.util.Objects;

import org.littletonrobotics.junction.LogTable;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.inputs.LoggableInputs;

/**
 * A wrapper class for {@link TalonFX}.
 * Uses the <a href="https://github.com/Mechanical-Advantage/AdvantageKit">AdvantageKit</a> library to log many aspects of the motor,
 * and will automatically alert the user if a motor becomes disconnected.
 * <p>
 * If tuning is enabled for this motor, allows tuning of PID and Feedforward.
 */
public class BulldogTalonFX extends LoggableMotor {

  /** 
   * The backing motor of this BulldogTalonFX.
   * Should only be accessed to control the motor in a way the library has not accounted for.
   */
  public final TalonFX motor;
  private final StatusSignal<Voltage> appliedVoltage;
  private final StatusSignal<Current> supplyCurrent;
  private final StatusSignal<Angle> position;
  private final StatusSignal<AngularVelocity> velocity;
  private final StatusSignal<AngularVelocity> rotorVelocity;
  private final StatusSignal<Temperature> tempurature;

  private final TalonFXConfiguration config;

  private double loggedAppliedVoltage;
  private double loggedSupplyCurrent;
  private double loggedPosition;
  private double loggedVelocity;
  private double loggedRotorVelocity;
  private double loggedTempurature;
  private boolean loggedConnected;

  private final BulldogTunableNumber kP;
  private final BulldogTunableNumber kI;
  private final BulldogTunableNumber kD;
  private final BulldogTunableNumber kS;
  private final BulldogTunableNumber kV;
  private final BulldogTunableNumber kA;
  private final BulldogTunableNumber kG;

  private final Alert disconnectedAlert;
  private final Debouncer alertDebouncer = new Debouncer(0.5, DebounceType.kFalling);

  /**
   * Creates a new BulldogTalonFX Wrapper with the given ID, the given name, and the given configs.
   *
   * @param id The ID of the TalonFX.
   * @param name The name of the BulldogTalonFX.
   * @param config The configs to give to the TalonFX.
   * @param enableTuning Whether or not to enable tuning of the motor.
   *    If enabled, will allow tuning of PID and Feedforward values.
   * @throws IllegalArgumentException if the ID is not between [0, 62].
   * @throws IllegalArgumentException if the name is null, empty, or contains only whitespace characters.
   * @throws NullPointerException if the config is null.
   */
  public BulldogTalonFX(int id, String name, TalonFXConfiguration config, boolean enableTuning) {
    super(name, id);
    super.setInputs(
      new LoggableInputs() {
          public void toLog(LogTable table) {
            table.put("AppliedVoltage", loggedAppliedVoltage);
            table.put("SupplyCurrent", loggedSupplyCurrent);
            table.put("Position", loggedPosition);
            table.put("Velocity", loggedVelocity);
            table.put("RotorVelocity", loggedRotorVelocity);
            table.put("Tempurature", loggedTempurature);
            table.put("Connected", loggedConnected);
          }

          public void fromLog(LogTable table) {
            loggedAppliedVoltage = table.get("AppliedVoltage", 0);
            loggedSupplyCurrent = table.get("SupplyCurrent", 0);
            loggedPosition = table.get("Position", 0);
            loggedVelocity = table.get("Velocity", 0);
            loggedRotorVelocity = table.get("RotorVelocity", 0);
            loggedTempurature = table.get("Tempurature", 0);
            loggedConnected = table.get("Connected", false);
          }
        }
    );
    
    this.config = Objects.requireNonNull(config, "Config must not be null!");

    motor = new TalonFX(id);
    appliedVoltage = motor.getMotorVoltage();
    supplyCurrent = motor.getSupplyCurrent();
    position = motor.getPosition();
    velocity = motor.getVelocity();
    rotorVelocity = motor.getRotorVelocity();
    tempurature = motor.getDeviceTemp();

    motor.getConfigurator().apply(this.config);

    Slot0Configs slot0Configs = this.config.Slot0;
    kP = new BulldogTunableNumber(name + "/kP", slot0Configs.kP, enableTuning);
    kI = new BulldogTunableNumber(name + "/kI", slot0Configs.kI, enableTuning);
    kD = new BulldogTunableNumber(name + "/kD", slot0Configs.kD, enableTuning);
    kS = new BulldogTunableNumber(name + "/kS", slot0Configs.kS, enableTuning);
    kV = new BulldogTunableNumber(name + "/kV", slot0Configs.kV, enableTuning);
    kA = new BulldogTunableNumber(name + "/kA", slot0Configs.kA, enableTuning);
    kG = new BulldogTunableNumber(name + "/kG", slot0Configs.kG, enableTuning);

    disconnectedAlert = new Alert(name + " disconnected!", AlertType.kError);
  }

  /**
   * Updates and processes the inputs of the motor. Also will alert the user if the motor is disconnected.
   * <p>
   * Called periodically (every loop) as part of {@link BulldogTalonFX#updateAllMotors}
   */
  @Override
  protected void update() {
    // If the logger DOES have a replay source, the logged values will be updated from the logs.
    if (!Logger.hasReplaySource()) {
      loggedAppliedVoltage = appliedVoltage.getValueAsDouble();
      loggedSupplyCurrent = supplyCurrent.getValueAsDouble();
      loggedPosition = position.getValueAsDouble();
      loggedVelocity = velocity.getValueAsDouble();
      loggedRotorVelocity = rotorVelocity.getValueAsDouble();
      loggedTempurature = tempurature.getValueAsDouble();
      loggedConnected = motor.isConnected();
    }

    super.update();

    BulldogTunableNumber.ifChanged(
      hashCode(),
      (values) -> {
        config.Slot0 = config.Slot0
          .withKP(values[0])
          .withKI(values[1])
          .withKD(values[2])
          .withKS(values[3])
          .withKV(values[4])
          .withKA(values[5])
          .withKG(values[6]);

        motor.getConfigurator().apply(config);
      }, 
      kP, kI, kD, kS, kV, kA, kG);

    disconnectedAlert.set(alertDebouncer.calculate(!loggedConnected));
  }

  /**
   * Creates a copy of this BulldogTalonFX with the applied Follower ControlRequest.
   * @param leaderID The ID of the motor to follow.
   * @param opposeLeader {@code true} to oppose the output of the leader, {@code false} to align to the output of the leader.
   * @return A copy of this BulldogTalonFX, following the motor at the given ID.
   * @throws IllegalArgumentException if the motor is trying to follow itself (i.e. leaderID equals the backing motor's ID).
   */
  public BulldogTalonFX withLeader(int leaderID, boolean opposeLeader) {
    if (leaderID == motor.getDeviceID()) throw new IllegalArgumentException("A motor cannot follow itself!");
    motor.setControl(new Follower(leaderID, opposeLeader ? MotorAlignmentValue.Opposed : MotorAlignmentValue.Aligned));
    return this;
  }

  /**
   * Creates a copy of this BulldogTalonFX with the applied Follower ControlRequest.
   * @param leaderMotor The BulldogTalonFX to follow.
   * @param opposeLeader {@code true} to oppose the output of the leader, {@code false} to align to the output of the leader.
   * @return A copy of this BulldogTalonFX, following the given BulldogTalonFX
   * @throws IllegalArgumentException if the motor is trying to follow itself.
   * @throws NullPointerException if trying to follow a null motor.
   */
  public BulldogTalonFX withLeader(BulldogTalonFX leaderMotor, boolean opposeLeader) {
    return this.withLeader(Objects.requireNonNull(leaderMotor, "Cannot follow a null motor!").motor.getDeviceID(), opposeLeader);
  }

  /**
   * Creates a copy of this BulldogTalonFX with the applied NeutralMode config.
   * @param value {@code true} to enable brake mode, {@code false} to enable coast mode.
   * @return A copy of this BulldogTalonFX, with the desired NeutralMode config.
   */
  public BulldogTalonFX withBrakeMode(boolean value) {
    config.MotorOutput.NeutralMode = value ? NeutralModeValue.Brake : NeutralModeValue.Coast;
    motor.getConfigurator().apply(config);
    return this;
  }

  /**
   * Creates a copy of this BulldogTalonFX with the applied InvertedMode config.
   * @param value {@code true} for Clockwise_Positive, {@code false} for CounterClockwise_Postive.
   * @return A copy of this BulldogTalonFX, with the desired InvertedMode config.
   */
  public BulldogTalonFX withClockwisePositive(boolean value) {
    config.MotorOutput.Inverted = value ? InvertedValue.Clockwise_Positive : InvertedValue.CounterClockwise_Positive;
    motor.getConfigurator().apply(config);
    return this;
  }

  /**
   * Set the speed of the motor.
   * @param output The speed to set. Should be between [-1.0, 1.0].
   * @see TalonFX#set(double)
   */
  public void set(double output) {
    if (output < -1) output = -1;
    else if (output > 1) output = 1;
    motor.set(output);
  }

  /**
   * Requests neutral output of the motor.
   */
  public void stop() {
    setControl(new NeutralOut());
  }

  /**
   * Control the motor with the given ControlRequest.
   * @param control The ControlRequest to pass to the motor.
   * @throws NullPointerException if trying to follow a null ControlRequest.
   * @see TalonFX#setControl(ControlRequest)
   */
  public void setControl(ControlRequest control) {
    motor.setControl(Objects.requireNonNull(control, "ControlRequest must not be null!"));
  }

  /**
   * Resets the motor's position to the given value.
   * @param position The angle to reset the motor to. Will be converted to Rotations.
   * @throws NullPointerException if trying to reset to a null position.
   * @see TalonFX#setPosition(Angle)
   */
  public void resetPosition(Angle position) {
    motor.setPosition(Objects.requireNonNull(position, "Position must not be null!"));
  }

  /**
   * Resets the motor's position to the given value.
   * @param position The position to reset the motor to, in Rotations.
   * @see TalonFX#setPosition(Angle)
   */
  public void resetPosition(double position) {
    motor.setPosition(position);
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
   * Gets the position of the mechanism.
   * This value IS affected by RotorToSensorRatio and SensorToMechanismRatio configs applied to the backing motor.
   * @return The position, in Rotations.
   */
  public double getMechanismPosition() {
    return loggedPosition;
  }

  /**
   * Gets the velocity of the mechanism.
   * This value IS affected by RotorToSensorRatio and SensorToMechanismRatio configs applied to the backing motor.
   * @return The velocity, in Rotations per Second.
   */
  public double getMechanismVelocity() {
    return loggedVelocity;
  }

  /**
   * Gets the velocity of the motor's rotor.
   * This value is NOT affected by RotorToSensorRatio and SensorToMechanismRatio configs applied to the backing motor.
   * @return The rotor velocity, in Rotations per Second.
   */
  public double getRotorVelocity() {
    return loggedRotorVelocity;
  }

  /**
   * Gets the tempurature of the motor.
   * @return The tempurature, in Celsius.
   */
  public double getTempurature() {
    return loggedTempurature;
  }

  /**
   * Returns whether the motor is connected to the robot.
   * @return {@code true} if the motor is connected, {@code false} otherwise.
   */
  public boolean isConnected() {
    return loggedConnected;
  }

}
