package operations;

import reservationStation.ReservationStation;
import basicUnits.ClockDependentUnit;
import basicUnits.instruction.Instruction;

//TODO change it so that the operation is done at the beginning (so we don't need the register values anymore) and put the values in a "pipline" to mirror the actual pipeline.
public abstract class OperationalUnit extends ClockDependentUnit
{
	public final int minLatency;
	public final boolean isPipelineable;
	protected final Instruction[] pipeline;
	protected final Number[] solutions;
	private boolean outputIsReady;
	private boolean outputIsTaken;
//	private Number output;
	
	private final ReservationStation reservationStation;
	
	public OperationalUnit(int minLatency, boolean isPipelineable, ReservationStation reservationStation)
	{
		super(1);
		this.minLatency = minLatency;
		this.isPipelineable = isPipelineable;
		this.pipeline = new Instruction[this.minLatency+1];
		this.solutions = new Number[this.minLatency];
		this.reservationStation = reservationStation;
	}
	
	public boolean isOutputReady()
	{
		return this.outputIsReady;
	}
	
	public Instruction getOutputInstruction()
	{
		return this.pipeline[this.pipeline.length-1];
	}
	
	public Number getOutput()
	{
		if(!this.outputIsReady)
		{
			return null;
		}
		this.outputIsTaken = true;
		return this.solutions[this.solutions.length-1];
	}
	
	public boolean isReadyForInput()
	{
		if(this.isPipelineable)
		{
			return this.pipeline[0] == null;
		}
		
		//checking non-pipelineable units
		for(int i = 0; i < this.pipeline.length; i++)
		{
			if(this.pipeline[i] != null)
			{
				return false;
			}
		}
		return true;
	}
	
	public boolean prepInstructionForPipeline()
	{
		if(!this.isReadyForInput())
		{
			return false;
		}
		
		//TODO check if registers are ready to be read
		this.pipeline[0] = this.reservationStation.getInstruction();
		return this.pipeline[0] != null;
	}
	
	@Override
	public void newClockCycle()
	{
		super.newClockCycle();
		
		//if output is taken, then discard it and allow another to take its place
		if(this.outputIsTaken)
		{
			this.outputIsReady = false;
			this.outputIsTaken = false;
			this.solutions[this.solutions.length-1] = null;
			this.pipeline[this.pipeline.length-1] = null;
		}
		
		//move pipeline
		for(int i = this.pipeline.length-2; i > 0; i--)
		{
			if(this.pipeline[i+1] == null)
			{
				this.pipeline[i+1] = this.pipeline[i];
				this.pipeline[i] = null;
				this.solutions[i] = this.solutions[i-1];
				this.solutions[i-1] = null;
			}
		}
		
		if(this.pipeline[0] != null)
		{
			this.doOneOperation();
		}
	}
	
	/**
	 * 
	 */
	@Override
	public boolean doOneOperation()
	{
		if(this.pipeline[0] != null && this.pipeline[1] == null && super.doOneOperation())
		{
			this.solutions[0] = this.operate();
			this.pipeline[1] = this.pipeline[0];
			return true;
		}
		return false;
	}
	
	protected abstract Number operate();
}
