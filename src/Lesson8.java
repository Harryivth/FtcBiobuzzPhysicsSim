import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.io.IOException;

public class Lesson8 {
	public static double targetHeight = 1.513; //m
	public static double launchHeight = 0.322;  //m
	public static double timeStep = 0.0001;


	public static void main(String[] args) throws IOException {
		Instant start = Instant.now();


		ArrayList<BestShot> shotTable = buildShotTable(0, 5.5, 0.05);
		ShotTableExporter.exportShotTable(shotTable);

// Test 1: robot moving sideways at 1 m/s
		MovingShot movingTest = calculateMovingShot(shotTable, 0, 0, 1.0, 0.0, 0, 3);
		System.out.printf("MOVING     aim: %.1f  distance: %.2f  speed: %.3f  rpm: %.0f  servo: %.3f%n",
				movingTest.aimAngleDegrees,
				movingTest.distanceToVirtualTarget,
				movingTest.bestShot.launchSpeed,
				calculateWheelRpm(movingTest.bestShot.launchSpeed),
				calculateServoPosition(movingTest.bestShot.launchAngleDegrees));

// Test 2: robot standing still
		MovingShot stationaryTest = calculateMovingShot(shotTable, 0, 0, 0.0, 0.0, 0, 3);
		System.out.printf("STATIONARY aim: %.1f  distance: %.2f  speed: %.3f  rpm: %.0f  servo: %.3f%n",
				stationaryTest.aimAngleDegrees,
				stationaryTest.distanceToVirtualTarget,
				stationaryTest.bestShot.launchSpeed,
				calculateWheelRpm(stationaryTest.bestShot.launchSpeed),
				calculateServoPosition(stationaryTest.bestShot.launchAngleDegrees));

		Instant end = Instant.now();
		System.out.printf("calculationTimeMS:%d%n", Duration.between(start,end).toMillis());
	}


	static double calculateLaunchSpeedWithDrag(double distanceToTarget, double targetHeight, double launchHeight, double launchAngleDegrees){
		double slowestGuess = 1.0;
		double fastestGuess = 30.0;
		double checkAmount = 30;


		//bisection search off of Google
		for (int guessNumber = 0; guessNumber < checkAmount; guessNumber++){
			double middleGuess = (slowestGuess + fastestGuess) / 2;
			double heightAtTarget = simulateFlight(middleGuess, launchAngleDegrees, distanceToTarget, launchHeight).heightAtTarget;

			if(heightAtTarget < targetHeight){
				slowestGuess = middleGuess;
			}else{
				fastestGuess = middleGuess;
			}

		}
		return (slowestGuess + fastestGuess) / 2;
	}


	static class FlightResult{
		double heightAtTarget;
		double entryAngleDegrees;
		double flightTimeSeconds;
	}

	private static FlightResult simulateFlight(double launchSpeed, double launchAngleDegrees, double distanceToTarget, double launchHeight){
		double dragCoefficient = 0.45;
		double ballMass = 0.02494758; //kg
		double ballDiameter = 0.07111999999999999; //m
		double ballRadius = ballDiameter / 2; //m
		double ballCrossSectionArea = Math.PI * Math.pow(ballRadius,2);
		double flightTimeSeconds = 0;

		double dragFactor = 0.5 * WorldConstants.AIR_DENSITY * dragCoefficient * ballCrossSectionArea / ballMass;

		double ballX = 0;
		double ballY = launchHeight;
		double ballXVelocity = launchSpeed * Math.cos(Math.toRadians(launchAngleDegrees));
		double ballYVelocity = launchSpeed * Math.sin(Math.toRadians(launchAngleDegrees));



		while (ballX < distanceToTarget && ballY > 0){
			double ballSpeed = Math.sqrt(Math.pow(ballXVelocity,2) + Math.pow(ballYVelocity,2));
			ballX = ballX + ballXVelocity * timeStep;
			ballY = ballY + ballYVelocity * timeStep;
			ballXVelocity = ballXVelocity - dragFactor * ballSpeed * ballXVelocity * timeStep;
			ballYVelocity = ballYVelocity - (WorldConstants.GRAVITY +dragFactor * ballSpeed * ballYVelocity) * timeStep;
			flightTimeSeconds = flightTimeSeconds + timeStep;
		}
		double entryAngleDegrees = Math.toDegrees(Math.atan2(-ballYVelocity, ballXVelocity));
		FlightResult flightResult = new FlightResult();
		flightResult.heightAtTarget = ballY;
		flightResult.entryAngleDegrees = entryAngleDegrees;
		flightResult.flightTimeSeconds = flightTimeSeconds;
		return flightResult;
	}


	static class BestShot {
		double launchAngleDegrees;
		double launchSpeed;
		double entryAngleDegrees;
		double flightTimeSeconds;
		double distanceToTarget;
	}

	//uses the hood and flywheel to find the shot that will use the least flywheel speed
	static BestShot findBestShot(double distanceToTarget, double targetHeight, double launchHeight){
		double ballDiameterInches = 2.8;
		double marginInches = 1.0;
		double openingWidthInches = 20;
		double minimumEntryAngleDegrees = Math.toDegrees(Math.asin((ballDiameterInches + marginInches) / openingWidthInches));

		double maximumWheelRpm = 6000;      // your motor's free speed after any gear ratio

		double maximumLaunchSpeed = 0.85 * RobotConstants.speedEfficiency * Math.PI * RobotConstants.wheelDiameterMeters * maximumWheelRpm / 60;




		BestShot bestShot = null;

		for(double launchAngleDegrees = RobotConstants.minimumHoodAngleDegrees; launchAngleDegrees <= RobotConstants.maximumHoodAngleDegrees; launchAngleDegrees +=0.25){//change that 0.25 to be 1 if it gets to laggy
			double launchSpeed = calculateLaunchSpeedWithDrag(distanceToTarget, targetHeight, launchHeight, launchAngleDegrees);

			if(launchSpeed > maximumLaunchSpeed){
				continue;
			}

			FlightResult flightResult = simulateFlight(launchSpeed, launchAngleDegrees, distanceToTarget, launchHeight);

			if(flightResult.entryAngleDegrees < minimumEntryAngleDegrees){
				continue;
			}

			if(bestShot == null || launchSpeed < bestShot.launchSpeed){
				bestShot = new BestShot();
				bestShot.launchAngleDegrees = launchAngleDegrees;
				bestShot.launchSpeed = launchSpeed;
				bestShot.entryAngleDegrees = flightResult.entryAngleDegrees;
				bestShot.flightTimeSeconds = flightResult.flightTimeSeconds;
			}
		}
		return bestShot;
	}



	static class MovingShot {
		double aimAngleDegrees;   // Angle to turn the shooter too
		double distanceToVirtualTarget;
		BestShot bestShot;
	}

	static MovingShot calculateMovingShot(ArrayList<BestShot> shotTable, double shooterX, double shooterY,
										  double robotVelocityX, double robotVelocityY, double targetX, double targetY) {
		double virtualTargetX = targetX;
		double virtualTargetY = targetY;

		for (int round = 0; round < 4; round++) {
			double distance = Math.hypot(virtualTargetX - shooterX, virtualTargetY - shooterY);
			BestShot bestShot = lookupShot(shotTable, distance);
			virtualTargetX = targetX - robotVelocityX * bestShot.flightTimeSeconds;
			virtualTargetY = targetY - robotVelocityY * bestShot.flightTimeSeconds;
		}

		MovingShot movingShot = new MovingShot();
		movingShot.distanceToVirtualTarget = Math.hypot(virtualTargetX - shooterX, virtualTargetY - shooterY);
		movingShot.bestShot = lookupShot(shotTable, movingShot.distanceToVirtualTarget);
		movingShot.aimAngleDegrees = Math.toDegrees(Math.atan2(virtualTargetY - shooterY, virtualTargetX - shooterX));
		return movingShot;
	}


	static ArrayList<BestShot> buildShotTable(double minimumDistance, double maximumDistance, double distanceStep) {
		ArrayList<BestShot> shotTable = new ArrayList<>();
		int rowCount = (int) Math.round((maximumDistance - minimumDistance) / distanceStep) + 1;
		for (int rowNumber = 0; rowNumber < rowCount; rowNumber++) {
			double distance = minimumDistance + rowNumber * distanceStep;
			BestShot bestShot = findBestShot(distance,targetHeight,launchHeight);
			if (bestShot == null){
				continue;
			}
			bestShot.distanceToTarget = distance;
			shotTable.add(bestShot);
			// set distanceToTarget on the result, and add it to shotTable
		}
		return shotTable;
	}


	static double calculateWheelRpm(double launchSpeed){
		double wheelSurfaceSpeed = launchSpeed/ RobotConstants.speedEfficiency;
		return wheelSurfaceSpeed * 60 / (Math.PI * RobotConstants.wheelDiameterMeters);
	}

	static double calculateServoPosition(double launchAngleDegrees){
		double hoodFraction = (launchAngleDegrees - RobotConstants.minimumHoodAngleDegrees) / (RobotConstants.maximumHoodAngleDegrees - RobotConstants.minimumHoodAngleDegrees);
		return RobotConstants.servoPositionHoodUp + hoodFraction * (RobotConstants.servoPositionHoodDown - RobotConstants.servoPositionHoodUp);
	}

	static double interpolate(double lowerValue, double upperValue, double fraction){
		return lowerValue + fraction * (upperValue - lowerValue);
	}

	static BestShot lookupShot(ArrayList<BestShot> shotTable, double distance) {
		BestShot firstRow = shotTable.get(0);
		BestShot lastRow = shotTable.get(shotTable.size() - 1);
		if (distance <= firstRow.distanceToTarget) { return firstRow; }
		if (distance >= lastRow.distanceToTarget) { return lastRow; }

		for (int rowNumber = 0; rowNumber < shotTable.size() - 1; rowNumber++) {
			BestShot lowerRow = shotTable.get(rowNumber);
			BestShot upperRow = shotTable.get(rowNumber + 1);
			if (distance >= lowerRow.distanceToTarget && distance <= upperRow.distanceToTarget) {
				double fraction = (distance - lowerRow.distanceToTarget)
										  / (upperRow.distanceToTarget - lowerRow.distanceToTarget);

				BestShot interpolatedShot = new BestShot();
				interpolatedShot.distanceToTarget = distance;
				interpolatedShot.launchSpeed = interpolate(lowerRow.launchSpeed, upperRow.launchSpeed, fraction);
				interpolatedShot.launchAngleDegrees = interpolate(lowerRow.launchAngleDegrees, upperRow.launchAngleDegrees, fraction);
				interpolatedShot.flightTimeSeconds = interpolate(lowerRow.flightTimeSeconds, upperRow.flightTimeSeconds, fraction);
				return interpolatedShot;
			}
		}
		return lastRow;
	}
}
