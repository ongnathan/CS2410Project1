import java.util.*;

public class Instruction
{
	public int instructionNum; //Holds the order of the instruction
	public int cycleTime;
	public String operandOne; //will be stored as "R" or "F" and then a number representing the String 
	public String operandTwo; //Could be immediate value too or register with offset
	public String destination;
	public String op; //Stores the actual operation like "add"
	public boolean isBranch;
	
	
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
	
	
	
