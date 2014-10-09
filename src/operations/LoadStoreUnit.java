package operations;

import basicUnits.instruction.Instruction;
import basicUnits.instruction.InstructionType;
import reservationStation.ReservationStation;

public class LoadStoreUnit extends OperationalUnit
{
	boolean isStoreInstruction;
	
	public LoadStoreUnit(ReservationStation reservationStation)
	{
		super(2, false, reservationStation);
		this.isStoreInstruction = false;
	}
	
	@Override
	public boolean prepInstructionForPipeline()
	{
		if(!super.prepInstructionForPipeline())
		{
			return false;
		}
		
		if (super.pipeline[0].instructionType == InstructionType.SD || super.pipeline[0].instructionType == InstructionType.SPD)
		{
			this.isStoreInstruction = true;
		}
		else
		{
			this.isStoreInstruction = false;
		}
		return true;
	}
	
	@Override
	public void newClockCycle()
	{
		super.newClockCycle();
		
		//Store instruction skips to the end to take only one cycle to get to the end.
		if(this.isStoreInstruction && this.pipeline[1] != null)
		{
			this.pipeline[this.pipeline.length-1] = this.pipeline[1];
		}
	}
	
	//calculates the address
	@Override
	protected Number operate()
	{
		Instruction i = super.pipeline[super.pipeline.length-1];
		return i.operandOne.getValue().intValue() + i.immediate;
	}
}
