import java.time.Duration;
import java.time.Instant;

public class Lesson5 {

	public static final double GRAVITY = 9.81;
	public static final double AIR_DENSITY = 1.2;  //kg/m^3
	public static double distanceToTarget = 3;
	public static double targetHeight = 1.0;
	public static double launchHeight = 0.3;
	public static double launchAngleDegrees = 55;
	public static double timeStep = 0.0001;


	public static double dragCoefficient = 0.45;
	public static double ballMass = 0.02494758; //kg
	public static double ballDiameter = 0.07111999999999999; //m
	public static double ballRadius = ballDiameter / 2;
	public static double ballCrossSectionArea = Math.PI * Math.pow(ballRadius,2);

	public static double dragFactor = 0.5 * AIR_DENSITY * dragCoefficient * ballCrossSectionArea / ballMass;


	public static void main(String[] args) {
		Instant start = Instant.now();
		double launchSpeed = calculateLaunchSpeed(distanceToTarget,targetHeight,launchHeight,launchAngleDegrees);
		double ballX = 0;
		double ballY = launchHeight;
		double ballXVelocity = launchSpeed * Math.cos(Math.toRadians(launchAngleDegrees));
		double ballYVelocity = launchSpeed * Math.sin(Math.toRadians(launchAngleDegrees));



		while (ballX < distanceToTarget){
			double ballSpeed = Math.sqrt(Math.pow(ballXVelocity,2) + Math.pow(ballYVelocity,2));
			ballX = ballX + ballXVelocity * timeStep;
			ballY = ballY + ballYVelocity * timeStep;
			ballXVelocity = ballXVelocity - dragFactor * ballSpeed * ballXVelocity * timeStep;
			ballYVelocity = ballYVelocity - (GRAVITY +dragFactor * ballSpeed * ballYVelocity) * timeStep;


			System.out.printf("ballSpeed %.3f ballX %.3f ballY %.3f ballXVelocity %.3f ballYVelocity %.3f%n",ballSpeed, ballX, ballY, ballXVelocity, ballYVelocity);
		}
		double entryAngleDegrees = Math.toDegrees(Math.atan2(-ballYVelocity, ballXVelocity));
		System.out.printf("entryAngle %.2f%n",entryAngleDegrees);
		System.out.printf("launchSpeed %.2f%n",launchSpeed);
		Instant end = Instant.now();
		System.out.printf("calculationTimeMS:%d%n",Duration.between(start,end).toMillis());
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
}
