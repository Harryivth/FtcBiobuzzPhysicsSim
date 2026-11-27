import java.io.IOException;
import java.util.ArrayList;

public class ShotTableCalculator {
	public static void main(String[] args) throws IOException {
		ArrayList<BestShot> shotTable = buildShotTable(0, 5.5, 0.05);
		ShotTableExporter.exportShotTable(shotTable);
	}


	private static double calculateLaunchSpeedWithDrag(double distanceToTarget, double targetHeight,
											   double launchHeight, double launchAngleDegrees) {
		double slowestGuess = 1.0;
		double fastestguess = 30.0;
		double checkAmmount = 30;

		for(int guessNumber = 0; guessNumber < checkAmmount; guessNumber++){
			double middleGuess = (slowestGuess + fastestguess) / 2;
			double heightAtTarget = simulateFlight(middleGuess, launchAngleDegrees, distanceToTarget, launchHeight).heightAtTarget;

			if(heightAtTarget < targetHeight){
				slowestGuess = middleGuess;
			} else {
				fastestguess = middleGuess;
			}
		}
		return (slowestGuess + fastestguess) / 2;
	}


	static class FlightResult{
		double heightAtTarget;
		double entryAngleDegrees;
		double flightTimeSeconds;
	}

	private static FlightResult simulateFlight(double launchSpeed, double launchAngleDegrees,
											   double distanceToTarget, double launchHeight){
		double ballRadius = GameConstants.BALL_DIAMETER / 2;
		double ballCrossSectionArea = Math.PI * Math.pow(ballRadius, 2);
		double flightTimeSeconds = 0;

		double dragFactor = 0.5 * WorldConstants.AIR_DENSITY * GameConstants.DRAG_COEFFICIENT * ballCrossSectionArea / GameConstants.BALL_MASS;

		double ballX = 0;
		double ballY = launchHeight;
		double ballXVelocity = launchSpeed * Math.cos(Math.toRadians(launchAngleDegrees));
		double ballYVelocity = launchSpeed * Math.sin(Math.toRadians(launchAngleDegrees));

		while (ballX < distanceToTarget && ballY > 0){
			double ballSpeed = Math.sqrt(Math.pow(ballXVelocity,2) + Math.pow(ballYVelocity,2));
			ballX = ballX + ballXVelocity * WorldConstants.timeStep;
			ballY = ballY + ballYVelocity * WorldConstants.timeStep;
			ballXVelocity = ballXVelocity - dragFactor * ballSpeed * ballXVelocity * WorldConstants.timeStep;
			ballYVelocity = ballYVelocity - (WorldConstants.GRAVITY + dragFactor * ballSpeed * ballYVelocity) * WorldConstants.timeStep;
			flightTimeSeconds = flightTimeSeconds + WorldConstants.timeStep;
		}
		double entryAngleDegrees = Math.toDegrees(Math.atan2(-ballYVelocity,ballXVelocity));
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

	static BestShot findBestShot(double distanceToTarget, double targetHeight, double launchHeight){
		double marginInches = 1.0;
		double minimumEntryAngleDegrees = Math.toDegrees(Math.asin((GameConstants.BALL_DIAMETER_INCHES + marginInches) / GameConstants.OPENING_WIDTH_INCHES));

		double maximumLaunchSpeed = 0.85 * RobotConstants.speedEfficiency * Math.PI * RobotConstants.wheelDiameterMeters * RobotConstants.MAXIMUM_FLYWHEEL_RPM / 60;


		BestShot bestShot = null;

		for(double launchAngleDegrees = RobotConstants.minimumHoodAngleDegrees; launchAngleDegrees <= RobotConstants.maximumHoodAngleDegrees; launchAngleDegrees +=0.25){//change that 0.25 to be 1 if it gets to laggy
			double launchSpeed = calculateLaunchSpeedWithDrag(distanceToTarget, targetHeight, launchHeight, launchAngleDegrees);

			if(launchSpeed > maximumLaunchSpeed){
				continue;
			}

			ShotTableCalculator.FlightResult flightResult = simulateFlight(launchSpeed, launchAngleDegrees, distanceToTarget, launchHeight);

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

	static ArrayList<BestShot> buildShotTable(double minimumDistance, double maximumDistance, double distanceStep) {
		ArrayList<BestShot> shotTable = new ArrayList<>();
		int rowCount = (int) Math.round((maximumDistance - minimumDistance) / distanceStep) + 1;
		for (int rowNumber = 0; rowNumber < rowCount; rowNumber++) {
			double distance = minimumDistance + rowNumber * distanceStep;
			BestShot bestShot = findBestShot(distance,GameConstants.TARGET_HEIGHT,RobotConstants.SHOOTER_HEIGHT_OFFSET);
			if (bestShot == null){
				continue;
			}
			bestShot.distanceToTarget = distance;
			shotTable.add(bestShot);
			// set distanceToTarget on the result, and add it to shotTable
		}
		return shotTable;
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
