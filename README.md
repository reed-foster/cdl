# CDL

CDL - circuit description language  
repository by Reed Foster

## Description

The project is intended to be a learning experience in language/compiler theory. The intent of CDL is to provide a relatively beginner-friendly hardware description language that can be compiled into VHDL code which would then be synthesized to run on an FPGA.  
Originally, I'd hoped to actually produce a language that could be used as an alternative or in addition to VHDL. As I worked on the project more, I realized that, as is usual for my projects, the finished product is not very powerful. However, I think that this project has been a great learning experience for me in terms of how coding languages are designed and implemented.

## Usage

```$ cdl outputfile [sourcefiles]```

cdl is a small bash script that passes the filename arguments the user supplies to the compiled Java class, CDL.  
CDL opens each file and appends its contents to a list of components to parse and verify. It then compiles the source files and stores the output in the outputfile, overwriting any existing content.

## Specification

Check out the BNF specification for CDL [here](grammar.ebnf). This specification is richer than the implemented compiler, as I ran out of time/energy to implement all the features I originally set out to have (most notably generate and process statements; both very important paradigms)