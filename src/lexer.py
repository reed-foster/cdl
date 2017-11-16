ID, SIGASSIGN, VARASSIGN = ('ID', 'SIGASSIGN', 'VARASSIGN')
DECINTCONST, BININTCONST, HEXINTCONST, BINVECCONST, HEXVECCONST, BOOLCONST = ('DECINTCONST', 'BININTCONST', 'HEXINTCONST', 'BINVECCONST', 'HEXVECCONST', 'BOOLCONST')
ADD, SUB, MUL, DIV, MOD, EXP = ('ADD', 'SUB', 'MUL', 'DIV', 'MOD', 'EXP')
AND, OR, XOR, NOT = ('AND', 'OR', 'XOR', 'NOT')
LPAREN, RPAREN, LBRACE, RBRACE = ('LPAREN', 'RPAREN', 'LBRACE', 'RBRACE')
TERNQ, TERNSEP = ('TERNQ', 'TERNSEP')
LT, GT, LE, GE, EQ, NE = ('LT', 'GT', 'LE', 'GE', 'EQ', 'NE')
EOF, EOL = ('EOF', 'EOL')
COMMA = 'COMMA'

BOOLSCOPE, OTHERSCOPE = ('BOOLSCOPE', 'OTHERSCOPE')

class Token():
    def __init__(self, type, value):
        self.type = type
        self.value = value

    def __str__(self):
        return 'Token({type}, "{value}")'.format(type=self.type, value=self.value.__str__())


RESERVED_KEYWORDS = {
    'component' : Token('COMPONENT', 'component'),
    'port' : Token('PORT','port'),
    'arch' : Token('ARCH','arch'),
    'connect' : Token('CONNECT','connect'),
    'generate' : Token('GENERATE','generate'),
    'process' : Token('PROCESS', 'process'),
    'signal' : Token('SIGNAL','signal'),
    'variable' : Token('VARIABLE','variable'),
    'new' : Token('NEW','new'),
    'this' : Token('THIS','this'),
    'if' : Token('IF','if'),
    'for' : Token('FOR','for'),
    'while' : Token('WHILE','while'),
    'int' : Token('INT','int'),
    'vec' : Token('VEC','vec'),
    'bool' : Token('BOOL','bool'),
    'true' : Token(BOOLCONST, 'true'),
    'false' : Token(BOOLCONST, 'false'),
    'and' : Token('AND', 'and'),
    'or' : Token('OR', 'or'),
    'not' : Token('NOT', 'not'),
    'nand' : Token('NAND', 'nand'),
    'nor' : Token('NOR', 'nor'),
    'xor' : Token('XOR', 'xor'),
    'xnor' : Token('XNOR', 'xnor'),
}

class Lexer():
    def __init__(self, text):
        self.text = text
        self.pos = 0
        self.current_scopetype = OTHERSCOPE
        self.current_char = self.text[self.pos]

    def error(self):
        raise Exception('Invalid Character')

    def advance(self):
        self.pos += 1
        if self.pos > len(self.text) - 1:
            self.current_char = None
        else:
            self.current_char = self.text[self.pos]

    def peek(self):
        peek_pos = self.pos + 1
        if peek_pos > len(self.text) - 1:
            return None
        else:
            return self.text[peek_pos]

    def skipWhiteSpace(self):
        while self.current_char is not None and self.current_char.isspace():
            self.advance()

    def hexVector(self):
        result = ''
        while self.current_char is not None and (self.current_char.isdigit() or self.current_char in 'ABCDEabcdef'):
            result += self.current_char
            self.advance()
        self.advance()
        return result

    def binVector(self):
        result = ''
        while self.current_char is not None and self.current_char in '01':
            result += self.current_char
            self.advance()
        self.advance()
        return result

    def decInteger(self):
        result = ''
        while self.current_char is not None and self.current_char.isdigit():
            result += self.current_char
            self.advance()
        return result

    def binInteger(self):
        result = ''
        while self.current_char is not None and self.current_char in '01':
            result += self.current_char
            self.advance()
        return result

    def hexInteger(self):
        result = ''
        while self.current_char is not None and (self.current_char.isdigit() or self.current_char in 'ABCDEFabcdef'):
            result += self.current_char
            self.advance()
        return result

    def id(self):
        result = ''
        while self.current_char is not None and self.current_char.isalnum():
            result += self.current_char
            self.advance()
        token = RESERVED_KEYWORDS.get(result, Token(ID, result)) #dict.get(key[, default]) returns default if key isn't found
        return token

    def comment(self):
        while self.current_char is not None and self.current_char != '\n':
            self.advance()

    def getNextToken(self):
        while self.current_char is not None:

            if self.current_char.isspace():
                self.skipWhiteSpace()
                continue

            if self.current_char == ';':
                self.advance()
                return Token(EOL, ';')

            if self.current_char == ',':
                self.advance()
                return Token(COMMA, ',')

            if self.current_char == '/' and self.peek() == '/':
                self.comment()
                self.skipWhiteSpace()
                continue

            if self.current_char == 'x' and self.peek() == '"':
                self.advance()
                self.advance()
                return Token(HEXVECCONST, self.hexVector())

            if self.current_char == '"':
                self.advance()
                return Token(BINVECCONST, self.binVector())

            if self.current_char.isalpha():
                return self.id()

            if self.current_char.isdigit():
                if self.current_char == '0' and self.peek() == 'x':
                    self.advance()
                    self.advance()
                    return Token(HEXINTCONST, self.hexInteger())
                if self.current_char == '0' and self.peek() == 'b':
                    self.advance()
                    self.advance()
                    return Token(BININTCONST, self.binInteger())
                return Token(DECINTCONST, self.decInteger())

            # Tokenizing '<=' based on context (either relational operator - boolean context, or signal assignment - all other contexts)
            if self.current_char == '<' and self.peek() == '=':
                self.advance()
                self.advance()
                if self.current_scopetype == BOOLSCOPE:
                    return Token(LE, '<=')
                if self.current_scopetype == OTHERSCOPE:
                    return Token(SIGASSIGN, '<=')

            if self.current_char == ':' and self.peek() == '=':
                self.advance()
                self.advance()
                return Token(VARASSIGN, ':=')

            if self.current_char == '?':
                self.advance()
                return Token(TURNQ, '?')
            if self.current_char == ':':
                self.advance()
                return Token(TURNSEP, ':')

            # Tokenize relational operators
            if self.current_char == '>' and self.peek() == '=':
                self.advance()
                self.advance()
                return Token(GE, '>=')
            if self.current_char == '<':
                self.advance()
                return Token(LT, '<')
            if self.current_char == '>':
                self.advance()
                return Token(GT, '>')
            if self.current_char == '=' and self.peek() == '=':
                self.advance()
                self.advance()
                return Token(EQ, '==')
            if self.current_char == '!' and self.peek() == '=':
                self.advance()
                self.advance()
                return Token(NE, '!=')

            if self.current_char == '+':
                self.advance()
                return Token(ADD, '+')
            if self.current_char == '-':
                self.advance()
                return Token(SUB, '-')
            if self.current_char == '*' and self.peek() == '*':
                self.advance()
                self.advance()
                return Token(EXP, '**')
            if self.current_char == '*':
                self.advance()
                return Token(MUL, '*')
            if self.current_char == '/':
                self.advance()
                return Token(DIV, '/')
            if self.current_char == '%':
                self.advance()
                return Token(MOD, '%')     

            if self.current_char == '&':
                self.advance()
                return Token(AND, '&')
            if self.current_char == '|':
                self.advance()
                return Token(OR, '|')
            if self.current_char == '^':
                self.advance()
                return Token(XOR, '^')
            if self.current_char == '!':
                self.advance()
                return Token(NOT, '!')

            if self.current_char == '(':
                self.advance()
                return Token(LPAREN, '(')
            if self.current_char == ')':
                self.advance()
                return Token(RPAREN, ')')
            if self.current_char == '{':
                self.advance()
                return Token(LBRACE, '{')
            if self.current_char == '}':
                self.advance()
                return Token(RBRACE, '}')

            self.error()

        return Token(EOF, None)

lex = Lexer('hi <= x"12ffab"; lol := ( 0b1011); {banana <= true};')
for i in range(20):
    print(lex.getNextToken())