package net.calicoctl.bulldoglib.control;

import static edu.wpi.first.units.Units.Degrees;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

/** Tests for {@link BulldogTalonFX} */
public class BulldogTalonFXTests {

    @Test
    public void testDefaultConstructor() {
        assertDoesNotThrow(() -> new BulldogTalonFX(0));
        assertThrows(IllegalArgumentException.class, () -> new BulldogTalonFX(-1));
        assertThrows(IllegalArgumentException.class, () -> new BulldogTalonFX(99));
    }

    @Test
    public void testConstructorWithName() {
        assertDoesNotThrow(() -> new BulldogTalonFX(0, "TestMotor"));
        assertThrows(IllegalArgumentException.class, () -> new BulldogTalonFX(1, ""));
        assertThrows(IllegalArgumentException.class, () -> new BulldogTalonFX(1, null));
    }

    @Test
    public void testConstructorWithConfig() {
        TalonFXConfiguration testConfig = new TalonFXConfiguration();
        testConfig.Slot0.kP = 0.1;
        testConfig.Slot0.kD = 0.0;
        testConfig.MotorOutput.NeutralMode = NeutralModeValue.Brake;
        testConfig.MotorOutput.Inverted = InvertedValue.Clockwise_Positive;
        assertDoesNotThrow(() -> new BulldogTalonFX(0, "TestMotor", testConfig));
        assertThrows(NullPointerException.class, () -> new BulldogTalonFX(1, "TestMotor1", null));
    }

    @Test
    public void testFollowingCanID() {
        BulldogTalonFX testMotor = new BulldogTalonFX(0);
        assertDoesNotThrow(() -> testMotor.withLeader(1, false));
        assertThrows(IllegalArgumentException.class, () -> testMotor.withLeader(0, false));
    }

    @Test
    public void testFollowingBulldogTalonFX() {
        BulldogTalonFX testMotor = new BulldogTalonFX(0);
        assertDoesNotThrow(() -> testMotor.withLeader(new BulldogTalonFX(1), false));
        assertThrows(IllegalArgumentException.class, () -> testMotor.withLeader(testMotor, false));
        assertThrows(NullPointerException.class, () -> testMotor.withLeader(null, false));
    }

    @Test
    public void testSetControl() {
        BulldogTalonFX testMotor = new BulldogTalonFX(0);
        assertDoesNotThrow(() -> testMotor.setControl(new DutyCycleOut(0.5)));
        assertThrows(NullPointerException.class, () -> testMotor.setControl(null));
    }

    @Test
    public void testResetPositionToAngle() {
        BulldogTalonFX testMotor = new BulldogTalonFX(0);
        assertDoesNotThrow(() -> testMotor.resetPosition(Degrees.of(180)));
        assertThrows(NullPointerException.class, () -> testMotor.resetPosition(null));
    }
    
}
