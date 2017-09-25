class Bit:

    states = ['1', '0', 'z', 'x']

    def __init__(self, state = None):
        if state not in self.states:
            self.state = 'z'
        else:
            self.state = state

    def __str__(self):
        return self.state

    def _test(self):
        a = Bit('1')
        b = Bit('0')
        c = Bit('z')
        d = Bit('x')
        e = Bit('2')


class BitVector:

    def __init__(self, length, states = None):
        self.length = length
        if states == None:
            self.states = [Bit() for i in length]
        else:
            self.states = list(reversed([Bit(i) for i in states]))

    def __str__(self):
        return ''.join(list(reversed([i.state for i in self.states])))

    def bit(self, index):
        return self.states(index)

    def to_unsigned(self):
        return int(''.join(list(reversed([i.state for i in self.states]))), 2)

    def __add__(self, b):
        #add two bitvectors
        if self.length != b.length:
            raise ArithmeticError("Can't add bitvectors of different length")
            return None
        if 'z' in self.states or 'x' in self.states or 'z' in b.states or 'x' in b.states:
            return BitVector(self.length, ''.join(['x' for i in xrange(self.length)]))

        inta = self.to_unsigned()
        intb = b.to_unsigned()

        s = bin(inta + intb)[2:]

        if len(s) > self.length:
            s = s[len(s) - self.length:]

        return BitVector(self.length, '0' * (self.length - len(s)) + s)

    def __and__(self, b):
        #bitwise and
        if self.length != b.length:
            raise ArithmeticError("Can't and bitvectors of different length")
            return None
        if 'z' in self.states or 'x' in self.states or 'z' in b.states or 'x' in b.states:
            return BitVector(self.length, ''.join(['x' for i in xrange(self.length)]))

        inta = self.to_unsigned()
        intb = b.to_unsigned()

        s = bin(inta & intb)[2:]

        if len(s) > self.length:
            s = s[len(s) - self.length:]

        return BitVector(self.length, '0' * (self.length - len(s)) + s)


    def __xor__(self, b):
        #bitwise xor
        if self.length != b.length:
            raise ArithmeticError("Can't xor bitvectors of different length")
            return None
        if 'z' in self.states or 'x' in self.states or 'z' in b.states or 'x' in b.states:
            return BitVector(self.length, ''.join(['x' for i in xrange(self.length)]))

        inta = self.to_unsigned()
        intb = b.to_unsigned()

        s = bin(inta ^ intb)[2:]

        return BitVector(self.length, '0' * (self.length - len(s)) + s)

    def __eq__(self, b):
        if self.length != b.length:
            raise ArithmeticError("Can't compare bitvectors of different length")
            return None
        return self.states == b.states

    def __ne__(self, b):
        if self.length != b.length:
            raise ArithmeticError("Can't compare bitvectors of different length")
            return None
        return self.states != b.states

    @staticmethod
    def _test():
        a = BitVector(4, '1101')
        b = BitVector(4, '0001')
        c = a + b
        d = a ^ b
        e = a & b
        assert(c == '1110')
        assert(d == '1100')
        assert(e == '0001')

BitVector._test()
