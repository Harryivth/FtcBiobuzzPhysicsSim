public class Lesson3 {

	public static final double GRAVITY = 9.81;
	public static double distanceToTarget = 3;
	public static double targetHeight = 1.0;
	public static double launchHeight = 0.3;
	public static double launchAngleDegrees = 55;
	public static double timeStep = 0.001;

	public static void main(String[] args) {
		double launchSpeed = calculateLaunchSpeed(distanceToTarget,targetHeight,launchHeight,launchAngleDegrees);
		double ballX = 0;
		double ballY = launchHeight;
		double ballXVelocity = launchSpeed * Math.cos(Math.toRadians(launchAngleDegrees));
		double ballYVelocity = launchSpeed * Math.sin(Math.toRadians(launchAngleDegrees));


		while (ballX < distanceToTarget){
			ballX = ballX + ballXVelocity * timeStep;
			ballY = ballY + ballYVelocity * timeStep;
			ballYVelocity = ballYVelocity - GRAVITY * timeStep;

			System.out.printf("ballX%.2f ballY%.2f ballYVelocity%.2f%n",ballX, ballY, ballYVelocity);
		}
		double entryAngle = ballXVelocity - ballYVelocity;
		System.out.printf("entryAngle%.2f%n",entryAngle);
		System.out.printf("launchSpeed%.2f",launchSpeed);
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
