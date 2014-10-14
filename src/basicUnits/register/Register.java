package basicUnits.register;

import basicUnits.ClockDependentUnit;

public class Register<T extends Number> extends ClockDependentUnit
{
	private static final String[] PREFIXES = {"R", "F"};
	private T value;
	private final String name;
	
	public Register(T value, String name)
	{
		super(Integer.MAX_VALUE);
		this.name = name;
		this.value = value;
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
	
	public static void main(String[] args)
	{
		Register<Integer> r = new Register<Integer>(10, "R1");
		System.out.println(r.getValue());
		r.setValue(20);
		System.out.println(r.getValue());
		Register<Integer> r2 = new Register<Integer>(100, "R2");
		System.out.println(r2.getValue());
		r.setValue(r2.getValue());
		System.out.println(r.getValue());
	}
}
