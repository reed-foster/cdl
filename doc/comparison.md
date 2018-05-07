<!--
comparison.md - Reed Foster
a sample comparison between two circuits in CDL and VHDL
-->

```
component Foo
{
	port
	{
		input  vec a;
		input  vec b;
		input  vec c;
		input  vec d;
		output vec x;
	}

	arch
	{
		And andgate1 = new And(inputs = 2);
		And andgate2 = new And(inputs = 2);
		Or orgate = new Or(inputs = 2);

		connect
		{
			a => andgate1.a,
			b => andgate1.b,
			andgate1.p => orgate.a,

			c => andgate2.a,
			d => andgate2.b,
			andgate2.p => orgate.b,
			
			orgate.s => x
		}
	}
}
```

```vhdl
library ieee;
use ieee.std_logic_1164.all;

entity foo
	port
	(
		a : in  std_logic;
		b : in  std_logic;
		c : in  std_logic;
		d : in  std_logic;
		x : out std_logic
	);
end entity;

architecture structural of foo is

	signal a_and_b, c_and_d : std_logic := '0';

	component and2
		port
		(
			a : in  std_logic;
			b : in  std_logic;
			p : out std_logic
		);
	end component;

	component or2
		port
		(
			a : in  std_logic;
			b : in  std_logic;
			s : out std_logic
		);
	end component;

begin

	p1 : and2
		port map
		(
			a => a,
			b => b,
			p => a_and_b
		);

	p2 : and2
		port map
		(
			a => c,
			b => d,
			p => c_and_d
		);

	s1 : or2
		port map
		(
			a => a_and_b,
			b => c_and_d,
			s => x
		);

end architecture;
```