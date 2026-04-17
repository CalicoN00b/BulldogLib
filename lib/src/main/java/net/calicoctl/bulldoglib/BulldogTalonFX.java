package net.calicoctl.bulldoglib;

import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.hardware.TalonFX;

import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.math.filter.Debouncer.DebounceType;
import edu.wpi.first.units.measure.*;
import edu.wpi.first.wpilibj.Alert;
import edu.wpi.first.wpilibj.Alert.AlertType;

import java.util.HashSet;
import java.util.Set;

import org.littletonrobotics.junction.LogTable;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.inputs.LoggableInputs;

public class BulldogTalonFX {

  private static final Set<BulldogTalonFX> allMotors = new HashSet<>();

  public final TalonFX motor;
  private final StatusSignal<Voltage> appliedVoltage;
  private final StatusSignal<Current> supplyCurrent;
  private final StatusSignal<Angle> position;
  private final StatusSignal<AngularVelocity> velocity;
  private final StatusSignal<Temperature> tempurature;

  private double loggedAppliedVoltage;
  private double loggedSupplyCurrernt;
  private double loggedPosition;
  private double loggedVelocity;
  private double loggedTempurature;
  private boolean loggedConnected;

  private final String name;

  private final LoggableInputs inputs;

  private final Alert disconnectedAlert;
  private final Debouncer alertDebouncer = new Debouncer(0.5, DebounceType.kFalling);

  /**
   * Creates a new BulldogTalonFX Wrapper with the given id, a default name, and default configs.
   *
   * @param id The id of the TalonFX
   */
  public BulldogTalonFX(int id) {
    this(id, "Motor" + id);
  }

  /**
   * Creates a new BulldogTalonFX Wrapper with the given id, the given name, and default configs.
   *
   * @param id The id of the TalonFX
   * @param name The name of the BulldogTalonFX
   */
  public BulldogTalonFX(int id, String name) {
    this(id, name, new TalonFXConfiguration());
  }

  /**
   * Creates a new BulldogTalonFX Wrapper with the given id, the given name, and the given configs.
   *
   * @param id The id of the TalonFX
   * @param name The name of the BulldogTalonFX
   * @param config The configs to give to the TalonFX
   */
  public BulldogTalonFX(int id, String name, TalonFXConfiguration config) {
    motor = new TalonFX(id);
    appliedVoltage = motor.getMotorVoltage();
    supplyCurrent = motor.getSupplyCurrent();
    position = motor.getPosition();
    velocity = motor.getVelocity();
    tempurature = motor.getDeviceTemp();

    motor.getConfigurator().apply(config);

    this.name = name;

    inputs =
        new LoggableInputs() {
          public void toLog(LogTable table) {
            table.put("AppliedVoltage", loggedAppliedVoltage);
            table.put("SupplyCurrent", loggedSupplyCurrernt);
            table.put("Position", loggedPosition);
            table.put("Velocity", loggedVelocity);
            table.put("Tempurature", loggedTempurature);
            table.put("Connected", loggedConnected);
          }

          public void fromLog(LogTable table) {
            loggedAppliedVoltage = table.get("AppliedVoltage", 0);
            loggedSupplyCurrernt = table.get("SupplyCurrent", 0);
            loggedPosition = table.get("Position", 0);
            loggedVelocity = table.get("Velocity", 0);
            loggedTempurature = table.get("Tempurature", 0);
            loggedConnected = table.get("Connected", false);
          }
        };

    disconnectedAlert = new Alert(name + " disconnected!", AlertType.kWarning);

    allMotors.add(this);
  }

  /**
   * Updates the inputs of the motor and processes them.
   * <p>
   * Called periodically (every loop) as part of {@link BulldogTalonFX#updateAllMotors}
   */
  private void update() {
    // If the logger DOES have a replay source, the logged values will be updated from the logs.
    if (!Logger.hasReplaySource()) {
      loggedAppliedVoltage = appliedVoltage.getValueAsDouble();
      loggedSupplyCurrernt = supplyCurrent.getValueAsDouble();
      loggedPosition = position.getValueAsDouble();
      loggedVelocity = velocity.getValueAsDouble();
      loggedTempurature = tempurature.getValueAsDouble();
      loggedConnected = motor.isConnected();
    }

    Logger.processInputs("Motors/" + name, inputs);
    disconnectedAlert.set(alertDebouncer.calculate(!loggedConnected));
  }

  /**
   * Updates and processes the inputs all ALL registered BulldogTalonFX's.
   * <p>
   * <strong>MUST</strong> be called periodically (once every loop).
   * A convenient place to do so is in {@code Robot.robotPeriodic}.
   */
  public static void updateAllMotors() {
    for (BulldogTalonFX motor : allMotors) {
      motor.update();
    }
  }
}
