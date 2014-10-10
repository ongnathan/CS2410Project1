package basicUnits.instruction;

import basicUnits.register.Register;

public class Instruction
{
	private static int instructionCounter = 0;
	public final int instructionNum; //Holds the order of the instruction
	public final Register<? extends Number> operandOne; //will be stored as "R" or "F" and then a number representing the String 
	public final Register<? extends Number> operandTwo; //Could be immediate value too or register with offset
	public final Register<? extends Number> destination;
	public final InstructionType instructionType; //Stores the actual operation like "add"
	public final int immediate;
//	public final boolean isBranch;
	
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
	
	private Instruction(InstructionType instruction, Register<? extends Number> destination, Register<? extends Number> operandOne, Register<? extends Number> operandTwo, int immediate)
	{
		this.instructionNum = instructionCounter;
		instructionCounter++;
		
		this.instructionType = instruction;
		this.destination = destination;
		this.operandOne = operandOne;
		this.operandTwo = operandTwo;
		this.immediate = immediate;
	}
	
	public static InstructionType decodeInstructionFromString(String type)
	{
		type = type.replace(".", "P");
		return InstructionType.valueOf(type);
	}
	
	public String toString()
	{
		StringBuilder s = new StringBuilder();
		s.append("This is instruction number: " + instructionNum + "\n");
		s.append("The opcode is: " + instructionType.ordinal() + "\n");
		s.append("The operands are: " + destination + ", " + operandOne + ", " + operandTwo + "\n");
		return s.toString();
	}
}
