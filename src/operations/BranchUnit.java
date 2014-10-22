package operations;

import basicUnits.instruction.Instruction;
import reservationStation.ReservationStation;

public class BranchUnit extends OperationalUnit
{
	public BranchUnit(ReservationStation reservationStation)
	{
		super(1, true, reservationStation);
		// TODO Auto-generated constructor stub
	}
	
	//calculates whether or not the condition is satisfied.  1 = true, 0 = false
	//NOTE: the order of comparison will be DESTINATION operation OPERAND1
	//if no operand1, substitute with 0
	@Override
	protected Number operate()
	{
		Instruction i = super.pipeline[0];
		switch(i.instructionType)
		{
			case BEQ:
				return i.destination.getValue().intValue() == i.operandOne.getValue().intValue() ? 1 : 0;
			case BEQZ:
				return i.operandOne.getValue().intValue() == 0 ? 1 : 0;
			case BNE:
				return i.destination.getValue().intValue() != i.operandOne.getValue().intValue() ? 1 : 0;
			case BNEZ:
				return i.operandOne.getValue().intValue() != 0 ? 1 : 0;
			default:
				throw new UnsupportedOperationException("The requested operation is not handled by this unit.");
		}
	}
}
