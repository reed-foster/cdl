# Operator Precedence (highest precedence first, same precedence across a line)

+ power:      exp
+ factor:     not, sub (unary)
+ product:    mul, div, mod, (bitwise) and, nand, xor, xnor
+ sum:        add, sub, (bitwise) or
+ relation:   eq, ne, ge, gt, le, lt
+ boolexpr:   (boolean) xor, and, or, not