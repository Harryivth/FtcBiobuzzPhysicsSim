
public class Lesson1 {
	public static void main(String[] args) {
		final double GRAVITY = 9.81;
		double velocity = 10;  //  m/s
		double launchAngle = 45;
		final double SHOOTER_HEIGHT = 0.3;
//		final double shooterHeight = 0.314087; // METERS

		double theta = Math.toRadians(launchAngle);
		double velocityX = velocity * Math.cos(theta);
		double velocityY = velocity * Math.sin(theta);

		for(double time = 0; time <= 3; time += 0.1){
			double x = velocityX * time;
			double y = SHOOTER_HEIGHT + velocityY * time - 0.5 * GRAVITY * Math.pow(time,2);
			if (y < 0) break;
			System.out.printf("time=%.1f  x=%.2f  y=%.2f%n", time, x, y);
		}
	}
}
