package reservationStation;

import basicUnits.ClockDependentInterface;
import basicUnits.ClockDependentUnit;

public class ReservationStation extends ClockDependentUnit implements ClockDependentInterface
{

	public ReservationStation(int numOperationsPerCycle)
	{
		super(numOperationsPerCycle);
	}

}
