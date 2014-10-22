package operations;

import basicUnits.instruction.Instruction;
import reservationStation.ReservationStation;

public class DivisionUnit extends OperationalUnit
{
	public DivisionUnit(ReservationStation reservationStation)
	{
		super(8, false, reservationStation);
	}

	@Override
	protected Number operate()
	{
		Instruction i = super.pipeline[0];
		switch(i.instructionType)
		{
			case DIVPD:
				return i.operandOne.getValue().doubleValue() / i.operandTwo.getValue().doubleValue();
			default:
				throw new UnsupportedOperationException("The requested operation is not handled by this unit.");
		}
	}
}
