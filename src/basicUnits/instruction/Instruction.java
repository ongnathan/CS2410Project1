package basicUnits.instruction;

import basicUnits.register.Register;

public class Instruction
{
	public final int instructionNum; //Holds the order of the instruction
	public final int cycleTime;
	public final Register<?> operandOne; //will be stored as "R" or "F" and then a number representing the String 
	public final Register<?> operandTwo; //Could be immediate value too or register with offset
	public final Register<?> destination;
	public final String op; //Stores the actual operation like "add"
	public final boolean isBranch;
	
	
	public Instruction(int num, String reg1,String reg2,String dest, String code,boolean branch)
	{
		isBranch = branch;
		instructionNum = num;
		operandOne = reg1;
		operandTwo = reg2;
		destination = dest;
		op = code;
	}
	
	public String toString()
	{
		StringBuilder s = new StringBuilder();
		s.append("This is instruction number: " + instructionNum + "\n");
		s.append("The opcode is: " + op + "\n");
		s.append("The operands are: " + destination + ", " + operandOne + ", " + operandTwo + "\n");
		return s.toString();
	}
}
	
	
	
