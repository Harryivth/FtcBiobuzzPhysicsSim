import java.time.Duration;
import java.time.Instant;

public class Lesson6 {

	public static final double GRAVITY = 9.81;
	public static final double AIR_DENSITY = 1.2;  //kg/m^3
	public static double distanceToTarget = 3;
	public static double targetHeight = 1.0;
	public static double launchHeight = 0.3;
	public static double launchAngleDegrees = 55;
	public static double timeStep = 0.0001;





	public static void main(String[] args) {
		Instant start = Instant.now();

		for (double distance = 1.0; distance <= 3.5; distance += 0.25) {
			BestShot bestShot = findBestShot(distance, targetHeight, launchHeight);
			if (bestShot == null) {
				System.out.printf("distance %.2f m: no valid shot%n", distance);
			} else {
				System.out.printf("distance %.2f angle %.0f, speed %.3f, flight %.3f, entry %.1f%n",
						distance, bestShot.launchAngleDegrees, bestShot.launchSpeed, bestShot.flightTimeSeconds, bestShot.entryAngleDegrees);
			}
		}

		Instant end = Instant.now();
		System.out.printf("calculationTimeMS:%d%n", Duration.between(start,end).toMillis());
	}


	static double calculateLaunchSpeed(double distanceToTarget, double targetHeight, double launchHeight, double launchAngleDegrees) {
		double launchAngleRadians = Math.toRadians(launchAngleDegrees);

		double straightLineOvershoot = launchHeight + distanceToTarget * Math.tan(launchAngleRadians) - targetHeight;

		if (straightLineOvershoot <= 0) {
			return Double.NaN; // this angle can't reach the target
		}

		double launchSpeedSquared = GRAVITY * Math.pow(distanceToTarget, 2) / (2 * Math.pow(Math.cos(launchAngleRadians), 2) * straightLineOvershoot);
		return Math.sqrt(launchSpeedSquared);
	}

	static double calculateLaunchSpeedWithDrag(double distanceToTarget, double targetHeight, double launchHeight, double launchAngleDegrees){
		double slowestGuess = 1.0;
		double fastestGuess = 30.0;
		double checkAmount = 30;

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
//			System.out.printf("ballSpeed %.3f ballX %.3f ballY %.3f ballXVelocity %.3f ballYVelocity %.3f%n",ballSpeed, ballX, ballY, ballXVelocity, ballYVelocity);
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

	static BestShot findBestShot(double distanceToTarget, double targetHeight, double launchHeight){
		double minimumEntryAngleDegrees = 25; //change this to reflect the field dont forget lowk
		double maximumLaunchSpeed = 15; //will get changed to actual robot

		//shouldnt I put the max hood angles as the launch angles?

		BestShot bestShot = null;

		for(double launchAngleDegrees = 0; launchAngleDegrees <=65; launchAngleDegrees +=1){
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
}
