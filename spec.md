# Circuit Description Language (CDL) Specification

CDL is a high-level hardware description language that compiles to VHDL. It is strongly typed, but reduces some of structural VHDL's verbosity, and has slightly more strict syntax/style rules. Because CDL compiles to VHDL, both languages can be used together in a project.

## Base CDL

`input` - an input port, used in a component's port declaration  
`output` - an output port  
`inout` - a tristate port, like VHDL's `inout`  

`component` - similar to "structural" VHDL, most commonly used "component", can contain other components  
`basecomponent` - similar to "data-flow" VHDL, defined by a single function, used by components  

`port` - declaration of a component's inputs and outputs  
`components` - declaration of a components subcomponents  
`arch` - declaration of a component's architecture  
`func` - declaration of a basecomponent's functionality  

`connect` - connects components to each other  
`tristate` - connect an inout port to driver, drivee, and selector  
`drive` - connects a subcomponent input or component output  

`function` - defines a function that is run at compile-time  

## Utilities
`import` - used to import libraries of components, basecomponents, and functions  
`statemachine` - used to define a state machine  
`pipeline` - simplifies creation of pipelines  

## Built-in `basecomponents`
Synchronous built-in components are partially implemented in VHDL  
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

## Sample Code

```
import builtin.*;

component Fifo
{
    generic
    {
        uint dwidth = 8;
        uint depth = 16;
    }
    port
    {
        input d_in(8); // multi-bit ports are defined using name(size)
        input enqueue; // rather than using name(1) to define single-bit ports, CDL allows a port that is one bit wide to be defined simple as name
        input clock;
        input dequeue;
        output d_out(8);
    }

    components // component declaration includes VHDL's generic mapping
    {
        uint awidth = builtin.getbits(depth); // builtin.getbits(uint n) returns the required amount of bits to represent n

        Ram fiforam(ports=1, dwidth=dwidth, awidth=awidth, clear="none"); // synchronous built-in components allow the user to select clear modes ("none", "asynch", "synch")

        Adder enq_inc(width=awidth);
        Adder deq_inc(width=awidth);
        Reg enq_ptr(width=awidth);
        Reg deq_ptr(width=awidth);

        TFF enq_ptr_ovr(clear="none");
        TFF deq_ptr_ovr(clear="none");
        XOR full(inputs=2);
        NOT empty;

        AND enq_approver(inputs=2, inv0=true); // the invX argument to logic gate constructors allows for inversion (logical not) of inputs before they reach the gate
        AND deq_approver(inputs=2, inv0=true);
    }

    arch
    {
        // Connect fiforam
        connect(d_in, fiforam.d_in);
        connect(d_out, fiforam.d_out);
        connect(clock, fiforam.clock);
        connect(enq_approver.out, fiforam.store);
        drive(1, fiforam.enable);

        // Connect approver AND gates
        connect(full.out, enq_approver.in0);
        connect(enqueue, enq_approver.in1);
        connect(empty.out, deq_approver.in0);
        connect(dequeue, deq_approver.in1);

        // Counter logic
        connect(enq_ptr.out, enq_inc.a);
        connect(enq_approver.out, enq_ptr.en);
        connect(enq_inc.sum, enq_ptr.in);
        connect(enq_inc.cout, enq_ptr_ovr.t);
        drive(enq_inc.b, 1);

        connect(deq_ptr.out, deq_inc.a);
        connect(deq_approver.out, deq_ptr.en);
        connect(deq_inc.sum, deq_ptr.in);
        connect(deq_inc.cout, deq_ptr_ovr.t);
        drive(deq_inc.b, 1);

        // Empty/Full logic
        connect(enq_ptr_ovr.q, full.in0);
        connect(deq_ptr_ovr.q, full.in1);
        connect(full.out, empty.in);
    }
}
```
