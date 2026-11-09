import java.time.Duration;
import java.time.Instant;

public class Lesson7 {

	public static final double GRAVITY = 9.81; //m/s^2
	public static final double AIR_DENSITY = 1.2;  //kg/m^3
	public static double distanceToTarget = 3; //m
	public static double targetHeight = 1.513; //m
	public static double launchHeight = 0.322;  //m
	public static double launchAngleDegrees = 55;
	public static double timeStep = 0.0001;





	public static void main(String[] args) {
		Instant start = Instant.now();

		MovingShot movingShot = calculateMovingShot(0, 0, 0, 0, 0, 3);
		System.out.printf("aimAngleDegrees: %.1f%n", movingShot.aimAngleDegrees);
		System.out.printf("distanceToVirtualTarget: %.2f%n", movingShot.distanceToVirtualTarget);
		System.out.printf("launchSpeed: %.3f%n", movingShot.bestShot.launchSpeed);



//
//		for (double distance = 1.0; distance <= 3.9; distance += 0.25) {
//			BestShot bestShot = findBestShot(distance, targetHeight, launchHeight);
//			if (bestShot == null) {
//				System.out.printf("distance %.2f m: no valid shot%n", distance);
//			} else {
//				System.out.printf("distance %.2f angle %.0f, speed %.3f, flight %.3f, entry %.1f%n",
//						distance, bestShot.launchAngleDegrees, bestShot.launchSpeed, bestShot.flightTimeSeconds, bestShot.entryAngleDegrees);
//			}
//		}

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
		double ballRadius = ballDiameter / 2;
		double ballCrossSectionArea = Math.PI * Math.pow(ballRadius,2);
		double flightTimeSeconds = 0;

		double dragFactor = 0.5 * AIR_DENSITY * dragCoefficient * ballCrossSectionArea / ballMass;

		double ballX = 0;
		double ballY = launchHeight;
		double ballXVelocity = launchSpeed * Math.cos(Math.toRadians(launchAngleDegrees));
		double ballYVelocity = launchSpeed * Math.sin(Math.toRadians(launchAngleDegrees));



		while (ballX < distanceToTarget && ballY > 0){
			double ballSpeed = Math.sqrt(Math.pow(ballXVelocity,2) + Math.pow(ballYVelocity,2));
			ballX = ballX + ballXVelocity * timeStep;
			ballY = ballY + ballYVelocity * timeStep;
			ballXVelocity = ballXVelocity - dragFactor * ballSpeed * ballXVelocity * timeStep;
			ballYVelocity = ballYVelocity - (GRAVITY +dragFactor * ballSpeed * ballYVelocity) * timeStep;
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
	}

	//uses the hood and flywheel to find the shot that will use the least flywheel speed
	static BestShot findBestShot(double distanceToTarget, double targetHeight, double launchHeight){
		double ballDiameterInches = 2.8;
		double marginInches = 1.0;
		double openingWidthInches = 20;
		double minimumEntryAngleDegrees = Math.toDegrees(Math.asin((ballDiameterInches + marginInches) / openingWidthInches));
		double wheelDiameterMeters = 0.072;
		double maximumWheelRpm = 6000;      // your motor's free speed after any gear ratio
		double speedEfficiency = 0.55;      // placeholder, measure with test shots
		double maximumLaunchSpeed = 0.85 * speedEfficiency * Math.PI * wheelDiameterMeters * maximumWheelRpm / 60;
		double minimumHoodAngleDegrees = 90 - 46.84316;
		double maximumHoodAngleDegrees = 90 - 19.84316;


		BestShot bestShot = null;

		for(double launchAngleDegrees = minimumHoodAngleDegrees; launchAngleDegrees <= maximumHoodAngleDegrees; launchAngleDegrees +=1){
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

	static MovingShot calculateMovingShot(double shooterX, double shooterY,
										  double robotVelocityX, double robotVelocityY,
										  double targetX, double targetY) {
		double virtualTargetX = targetX;
		double virtualTargetY = targetY;

		for (int round = 0; round < 4; round++) {
			double distance = Math.hypot(virtualTargetX - shooterX, virtualTargetY - shooterY);
			BestShot bestShot = findBestShot(distance, targetHeight, launchHeight);
			virtualTargetX = targetX - robotVelocityX * bestShot.flightTimeSeconds;
			virtualTargetY = targetY - robotVelocityY * bestShot.flightTimeSeconds;
		}

		MovingShot movingShot = new MovingShot();
		movingShot.distanceToVirtualTarget = Math.hypot(virtualTargetX - shooterX, virtualTargetY - shooterY);
		movingShot.bestShot = findBestShot(movingShot.distanceToVirtualTarget, targetHeight, launchHeight);
		movingShot.aimAngleDegrees = Math.toDegrees(Math.atan2(virtualTargetY - shooterY, virtualTargetX - shooterX));
		return movingShot;
	}
}
