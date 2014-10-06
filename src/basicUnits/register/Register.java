package basicUnits.register;

import basicUnits.ClockDependentUnit;

public class Register<T extends Number> extends ClockDependentUnit
{
	private static final String[] PREFIXES = {"R", "F"};
	private T value;
	private String name;
	
	public Register(T value, String name)
	{
		super(Integer.MAX_VALUE);
	}
	
	public T getValue()
	{
		return value;
	}
	
	public void setValue(T value)
	{
		this.value = value;
	}
	
	@Override
	public String toString()
	{
		return this.name + " = " + this.value.toString();
	}
}
