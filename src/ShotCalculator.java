import java.time.Duration;
import java.time.Instant;

public class ShotCalculator {
	static final double INCHES_PER_METER = 39.3701;
	static final double SHOOTER_FORWARD_OFFSET_INCHES = 0.0;   // pivot ahead of robot center
	static final double SHOOTER_LEFT_OFFSET_INCHES = 0.0;      // pivot left of robot center



	public static class ShotCommand {
		double turretAngleDegrees;
		double wheelRpm;
		double servoPosition;
	}

	public static ShotCommand calculateShot(double robotXInches, double robotYInches, double headingDegrees,
											double robotVelocityXInchesPerSecond, double robotVelocityYInchesPerSecond, double targetXInches, double targetYInches){
		ShooterPosition shooterPosition = calculateShooterPosition(robotXInches, robotYInches, headingDegrees);
		MovingShot movingShot = calculateMovingShot(shooterPosition.xInches, shooterPosition.yInches,
				robotVelocityXInchesPerSecond, robotVelocityYInchesPerSecond, targetXInches, targetYInches);
		ShotCommand shotCommand = new ShotCommand();
		shotCommand.turretAngleDegrees = calculateTurretAngleDegrees(movingShot.aimAngleDegrees, headingDegrees);
		shotCommand.wheelRpm = movingShot.shotSetting.wheelRpm;
		shotCommand.servoPosition = movingShot.shotSetting.servoPosition;
		return shotCommand;
	}


	static class ShotSetting {
		double wheelRpm;
		double servoPosition;
		double flightTimeSeconds;
	}

	static double interpolate(double lowerValue, double upperValue, double fraction) {
		return lowerValue + fraction * (upperValue - lowerValue);
	}

	static ShotSetting lookupShotSetting(double distanceMeters) {
		double[] distances = ShotTableData.DISTANCE_METERS;
		int lastRowNumber = distances.length - 1;

		// Stay inside the table
		distanceMeters = Math.max(distances[0], Math.min(distanceMeters, distances[lastRowNumber]));

		// Find the row just below the distance
		int lowerRowNumber = 0;
		while (lowerRowNumber < lastRowNumber - 1 && distanceMeters > distances[lowerRowNumber + 1]) {
			lowerRowNumber++;
		}
		int upperRowNumber = lowerRowNumber + 1;

		double fraction = (distanceMeters - distances[lowerRowNumber])
								  / (distances[upperRowNumber] - distances[lowerRowNumber]);

		ShotSetting shotSetting = new ShotSetting();
		shotSetting.wheelRpm = interpolate(ShotTableData.WHEEL_RPM[lowerRowNumber],
				ShotTableData.WHEEL_RPM[upperRowNumber], fraction);
		shotSetting.servoPosition = interpolate(ShotTableData.SERVO_POSITION[lowerRowNumber],
				ShotTableData.SERVO_POSITION[upperRowNumber], fraction);
		shotSetting.flightTimeSeconds = interpolate(ShotTableData.FLIGHT_TIME_SECONDS[lowerRowNumber],
				ShotTableData.FLIGHT_TIME_SECONDS[upperRowNumber], fraction);
		return shotSetting;
	}

	static class ShooterPosition {
		double xInches;
		double yInches;
	}

	static ShooterPosition calculateShooterPosition(double robotXInches, double robotYInches, double headingDegrees) {
		double headingRadians = Math.toRadians(headingDegrees);
		ShooterPosition shooterPosition = new ShooterPosition();
		shooterPosition.xInches = robotXInches + SHOOTER_FORWARD_OFFSET_INCHES * Math.cos(headingRadians) - SHOOTER_LEFT_OFFSET_INCHES * Math.sin(headingRadians);
		shooterPosition.yInches = robotYInches + SHOOTER_FORWARD_OFFSET_INCHES * Math.sin(headingRadians) + SHOOTER_LEFT_OFFSET_INCHES * Math.cos(headingRadians);
		return shooterPosition;
	}


	static double calculateTurretAngleDegrees(double aimAngleDegrees, double headingDegrees){
		return aimAngleDegrees - headingDegrees;
	}

	static class MovingShot {
		double aimAngleDegrees;              // field angle to point the shooter
		double distanceToVirtualTargetMeters;
		ShotSetting shotSetting;
	}

	static MovingShot calculateMovingShot(double shooterXInches, double shooterYInches,
										  double robotVelocityXInchesPerSecond, double robotVelocityYInchesPerSecond,
										  double targetXInches, double targetYInches) {

		double shooterXMeters = shooterXInches / INCHES_PER_METER;
		double shooterYMeters = shooterYInches / INCHES_PER_METER;
		double robotVelocityXMetersPerSecond = robotVelocityXInchesPerSecond / INCHES_PER_METER;
		double robotVelocityYMetersPerSecond = robotVelocityYInchesPerSecond / INCHES_PER_METER;
		double targetXMeters = targetXInches / INCHES_PER_METER;
		double targetYMeters = targetYInches / INCHES_PER_METER;

		double virtualTargetXMeters = targetXMeters;
		double virtualTargetYMeters = targetYMeters;

		for (int round = 0; round < 4; round++) {
			double distanceMeters = Math.hypot(virtualTargetXMeters - shooterXMeters,
					virtualTargetYMeters - shooterYMeters);
			ShotSetting shotSetting = lookupShotSetting(distanceMeters);
			virtualTargetXMeters = targetXMeters - robotVelocityXMetersPerSecond * shotSetting.flightTimeSeconds;
			virtualTargetYMeters = targetYMeters - robotVelocityYMetersPerSecond * shotSetting.flightTimeSeconds;
		}

		MovingShot movingShot = new MovingShot();
		movingShot.distanceToVirtualTargetMeters = Math.hypot(virtualTargetXMeters - shooterXMeters,
				virtualTargetYMeters - shooterYMeters);
		movingShot.shotSetting = lookupShotSetting(movingShot.distanceToVirtualTargetMeters);
		movingShot.aimAngleDegrees = Math.toDegrees(Math.atan2(virtualTargetYMeters - shooterYMeters,
				virtualTargetXMeters - shooterXMeters));
		return movingShot;
	}

	public static void main(String[] args) {
		Instant start = Instant.now();
		ShotCommand testShot = calculateShot(70.75, 30, 90, 0, 0, 70.75, 100);
		Instant end = Instant.now();
		System.out.printf("calculationTimeMS:%d%n", Duration.between(start,end).toMillis());
		System.out.printf("TestShot   turret: %.1f  rpm: %.0f  servo: %.3f%n",
				 testShot.turretAngleDegrees, testShot.wheelRpm, testShot.servoPosition);

//		ShotCommand facingRight = calculateShot(70.75, 30, 0, 0, 0, 70.75, 100);
//		System.out.printf("FACING RIGHT   turret: %.1f  rpm: %.0f  servo: %.3f%n",
//				 facingRight.turretAngleDegrees, facingRight.wheelRpm, facingRight.servoPosition);
	}
}