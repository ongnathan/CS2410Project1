package operations;

import basicUnits.instruction.Instruction;
import reservationStation.ReservationStation;

public class MultiplicationUnit extends OperationalUnit
{
	public MultiplicationUnit(ReservationStation reservationStation)
	{
		super(3, true, reservationStation);
	}

	@Override
	protected Number operate()
	{
		Instruction i = super.pipeline[0];
		switch(i.instructionType)
		{
			case DMUL:
				return i.operandOne.getValue().intValue() * i.operandTwo.getValue().intValue();
			default:
				throw new UnsupportedOperationException("The requested operation is not handled by this unit.");
		}
	}
}
