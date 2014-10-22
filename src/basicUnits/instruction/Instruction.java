package basicUnits.instruction;
import basicUnits.register.Register;
public class Instruction
{
	private static int instructionCounter = 0;
	public int instructionNum; //Holds the order of the instruction
	public Register<? extends Number> operandOne; //will be stored as "R" or "F" and then a number representing the String 
	public Register<? extends Number> operandTwo; //Could be immediate value too or register with offset
	public final Register<? extends Number> destination;
	public final InstructionType instructionType; //Stores the actual operation like "add"
	public final int immediate;
	public String branchLabel;
	public boolean isReadyOne;
	public boolean isReadyTwo;// Is the instruction ready to be executed?
	//public boolean hasMoved;
	
	/**
	 * R-Type
	 * @param instructionName
	 * @param destination
	 * @param operandOne
	 * @param operandTwo
	 * @param cycleTime
	 */
	public Instruction(String instructionName, Register<? extends Number> destination, Register<? extends Number> operandOne, Register<? extends Number> operandTwo)
	{
		this(decodeInstructionFromString(instructionName),destination,operandOne,operandTwo,Integer.MIN_VALUE);
	}
	
	/**
	 * I-Type
	 * @param instructionName
	 * @param destination
	 * @param operandOne
	 * @param immediate
	 * @param cycleTime
	 */
	public Instruction(String instructionName, Register<? extends Number> destination, Register<? extends Number> operandOne, int immediate)
	{
		this(decodeInstructionFromString(instructionName),destination,operandOne,null,immediate);
	}
	
	public Instruction(InstructionType instruction, Register<? extends Number> destination, Register<? extends Number> operandOne, Register<? extends Number> operandTwo, int immediate)
	{
		this.isReadyOne = false;
		this.isReadyTwo = false;
		this.instructionNum = instructionCounter;
		instructionCounter++;
		
		this.instructionType = instruction;
		this.destination = destination;
		this.operandOne = operandOne;
		this.operandTwo = operandTwo;
		this.immediate = immediate;
	}
	public void setOpOne(Integer value)//still not working...
	{
		operandOne.setValue(value);
		this.isReadyOne = true;
	}
	public void setOpOne(Double value) //still not working...
	{
		operandOne.setValue(value);
		this.isReadyOne = true;
	}
	public void setOpTwo(Integer value)//still not working...
	{
		operandTwo.setValue(value);
		this.isReadyTwo = true;
	}
	public void setOpTwo(Double value)//still not working...
	{
		operandTwo.setValue(value);
		this.isReadyTwo = true;
	}
	
	public boolean isReadyToExecute()
	{
		return isReadyOne && isReadyTwo;
	}
	public static InstructionType decodeInstructionFromString(String type)
	{
		type = type.replaceAll("\\.", "P");
		return InstructionType.valueOf(type);
	}
	
	public void setLabel(String branch)
	{
		branchLabel = branch;
	}
	
	public String toString()
	{
		StringBuilder s = new StringBuilder();
		s.append("This is instruction number: " + instructionNum + "\n");
		s.append("The opcode is: " + instructionType + "\n");
		s.append("The operands are: " + destination + ", " + operandOne + ", " + operandTwo + "\n");
		s.append("The immediate value is: " + immediate + "\n");
		s.append(isReadyToExecute());
		return s.toString();
	}
}
