package operations;

import basicUnits.instruction.Instruction;
import reservationStation.ReservationStation;

public class IntegerUnit extends OperationalUnit
{
	public IntegerUnit(ReservationStation reservationStation)
	{
		super(1, true, reservationStation);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Number operate()
	{
		Instruction i = super.pipeline[super.pipeline.length];
		switch(i.instructionType)
		{
			case AND:
				return i.operandOne.getValue().intValue() & i.operandTwo.getValue().intValue();
			case ANDI:
				return i.operandOne.getValue().intValue() & i.immediate;
			case DADD:
				return i.operandOne.getValue().intValue() + i.operandTwo.getValue().intValue();
			case DADDI:
				return i.operandOne.getValue().intValue() + i.immediate;
			case DSUB:
				return i.operandOne.getValue().intValue() - i.operandTwo.getValue().intValue();
			case OR:
				return i.operandOne.getValue().intValue() | i.operandTwo.getValue().intValue();
			case ORI:
				return i.operandOne.getValue().intValue() | i.immediate;
			case SLT:
				return i.operandOne.getValue().intValue() < i.operandTwo.getValue().intValue() ? 1 : 0;
			case SLTI:
				return i.operandOne.getValue().intValue() < i.immediate ? 1 : 0;
			default:
				throw new UnsupportedOperationException("The requested operation is not handled by this unit.");
		}
//		return null;
	}
}
