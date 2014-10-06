package basicUnits.instruction;

import basicUnits.register.Register;

public class Instruction
{
	private static int instructionCounter = 0;
	public final int instructionNum; //Holds the order of the instruction
	public final Register<?> operandOne; //will be stored as "R" or "F" and then a number representing the String 
	public final Register<?> operandTwo; //Could be immediate value too or register with offset
	public final Register<?> destination;
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
	public Instruction(String instructionName, Register<?> destination, Register<?> operandOne, Register<?> operandTwo, int cycleTime)
	{
		this(decodeInstructionFromString(instructionName),destination,operandOne,operandTwo,Integer.MIN_VALUE,cycleTime);
	}
	
	/**
	 * I-Type
	 * @param instructionName
	 * @param destination
	 * @param operandOne
	 * @param immediate
	 * @param cycleTime
	 */
	public Instruction(String instructionName, Register<?> destination, Register<?>operandOne, int immediate, int cycleTime)
	{
		this(decodeInstructionFromString(instructionName),destination,operandOne,null,immediate,cycleTime);
	}
	
	private Instruction(InstructionType instruction, Register<?> destination, Register<?> operandOne, Register<?> operandTwo, int immediate, int cycleTime)
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
		type = type.replaceAll(".", "P");
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
