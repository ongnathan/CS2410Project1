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
	public boolean isINT()
	{
		return this == AND || this == OR || this == SLT || this == DADD || this == DSUB || this == DMUL;
	}
	public boolean isINTI()
	{
		return this == ANDI || this == ORI || this == SLTI || this == DADDI;
	}
	public boolean isINT1()
	{
		return this == AND || this == ANDI || this == OR || this == ORI || this == SLT || this == SLTI || this == DADD || this == DADDI || this == DSUB;
	}
	public boolean isFPMULDIV()
	{
		return isFPU() || isFPDIV();
	}
	public boolean isMULT()
	{
		return this == DMUL;
	}
	public boolean isLOAD()
	{
		return this == LD || this == SD || this==LPD || this == SPD;
	}
	public boolean isFPU()
	{
		return this == ADDPD || this == SUBPD || this == MULPD;
	}
	public boolean isFPDIV()
	{
		return this == DIVPD;
	}
	public boolean isStore()
	{
		return this == SD || this == SPD;
	}
}
