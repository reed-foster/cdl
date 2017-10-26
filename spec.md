# Circuit Description Language (CDL) Specification

CDL is a high-level hardware description language that compiles to VHDL. It is strongly typed, but reduces some of structural VHDL's verbosity, and has slightly more strict syntax/style rules. Because CDL compiles to VHDL, both languages can be used together in a project.

## Base CDL

`input` - an input port, used in a component's port declaration  
`output` - an output port  
`inout` - a tristate port, like VHDL's `inout`  

`component` - similar to a VHDL entity, contains a port declaration and architecture specification  

`port` - declaration of a component's inputs and outputs  
`components` - declaration of a components subcomponents  
`arch` - declaration of a component's architecture  
`test` - declaration of stimuli and assertion statements for verification of component functionality
 
`tristate` - connect an inout port to driver, drivee, and selector  

`function` - defines a function that is run at compile-time (typically defined outside of component)  
`type` - allows for user created types

## Utilities
`import` - used to import libraries of components, basecomponents, and functions  
`statemachine` - used to define a state machine  
`pipeline` - simplifies creation of pipelines  

## Built-ins
Built-in components are partially implemented in VHDL and/or CDL  
`AND` - bitwise and  
`OR` - bitwise or  
`NAND` - bitwise nand  
`NOR` - bitwise nor  
`NOT` - bitwise not  
`XOR` - bitwise xor  
`XNOR` - bitwise xnor  
`Reg` - configurable register  
`DFF` - d flip-flop  
`TFF` - t flip-flop  
`JKFF` - jk flip-flop  
`Adder` - configurable adder  
`Ram` - configurable ram block (single or multiple ports)  

