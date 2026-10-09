
public class Lesson2 {
	public static final double GRAVITY = 9.81;
public static double distanceToTarget = 3;
public static double targetHeight = 1.0;
public static double launchHeight = 0.3;
public static double launchAngleDegrees = 55;



	public static void main(String[] args) {
		double launchVelocity = calculateLaunchSpeed(distanceToTarget,targetHeight,launchHeight,launchAngleDegrees);

		double velocityX = launchVelocity * Math.cos(Math.toRadians(launchAngleDegrees));
		double velocityY = launchVelocity * Math.sin(Math.toRadians(launchAngleDegrees));
		double timeToTarget = distanceToTarget / velocityX;
		double heightAtTarget = launchHeight + velocityY * timeToTarget - 0.5 * GRAVITY * Math.pow(timeToTarget, 2);
		System.out.println(heightAtTarget);



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
