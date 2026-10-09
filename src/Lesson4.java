public class Lesson4 {
	public static final double AIR_DENSITY = 1.2;  //kg/m^3
	public static double dragCoefficient = 0.45;
	public static double ballMass = 0.02494758; //kg
	public static double ballDiamater = 0.07111999999999999; //m
	public static double ballRadius = ballDiamater / 2;
	public static double ballCrossSectionArea = Math.PI * Math.pow(ballRadius,2);
	public static void main(String[] args) {
		double dragFactor = 0.5 * AIR_DENSITY * dragCoefficient * ballCrossSectionArea / ballMass;
		System.out.println(dragFactor);

	}
}
