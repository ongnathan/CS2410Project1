package basicUnits.instruction;

public enum InstructionType
{
	AND, ANDI, OR, ORI, SLT, SLTI, DADD, DADDI, DSUB, DMUL, LD, SD, LPD, SPD, ADDPD, SUBPD, MULPD, DIVPD, BEQZ, BNEZ, BEQ, BNE;
	
	public boolean isBranch()
	{
		return this == BEQZ || this == BNEZ || this == BEQ || this == BNE;
	}
	
	public boolean isFloatingPointOperation()
	{
		return this == LPD || this == SPD || this == ADDPD || this == SUBPD || this == MULPD || this == DIVPD;
	}
}
