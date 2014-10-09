package operations;

import basicUnits.instruction.Instruction;
import reservationStation.ReservationStation;

public class FloatingPointUnit extends OperationalUnit
{
	public FloatingPointUnit(ReservationStation reservationStation)
	{
		super(4, true, reservationStation);
	}

	@Override
	protected Number operate()
	{
		Instruction i = super.pipeline[super.pipeline.length];
		switch(i.instructionType)
		{
			case ADDPD:
				return i.operandOne.getValue().doubleValue() + i.operandTwo.getValue().doubleValue();
			case SUBPD:
				return i.operandOne.getValue().doubleValue() - i.operandTwo.getValue().doubleValue();
			case MULPD:
				return i.operandOne.getValue().doubleValue() * i.operandTwo.getValue().doubleValue();
			default:
				throw new UnsupportedOperationException("The requested operation is not handled by this unit.");
		}
	}
}
