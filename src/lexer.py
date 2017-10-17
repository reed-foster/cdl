ID, SIGASSIGN, VARASSIGN = ('ID', 'SIGASSIGN', 'VARASSIGN')
INT, VEC, BOOL = ('INT', 'VEC', 'BOOL')
ADD, SUB, MUL, DIV, MOD = ('ADD', 'SUB', 'MUL', 'DIV', 'MOD')
LPAREN, RPAREN, LBRACE, RBRACE = ('LPAREN', 'RPAREN', 'LBRACE', 'RBRACE')
LT, GT, LE, GE, EQ, NE = ('LT', 'GT', 'LE', 'GE', 'EQ', 'NE')
EOF = 'EOF'

RESERVED_KEYWORDS = {
    'COMPONENT' : Token('COMPONENT', 'COMPONENT')
    'PORT' : Token('PORT','PORT'),
    'ARCH' : Token('ARCH','ARCH'),
    'CONNECT' : Token('CONNECT','CONNECT'),
    'GENERATE' : Token('GENERATE','GENERATE'),
    'SIGNAL' : Token('SIGNAL','SIGNAL'),
    'VARIABLE' : Token('VARIABLE','VARIABLE'),
    'NEW' : Token('NEW','NEW'),
    'THIS' : Token('THIS','THIS'),
    'IF' : Token('IF','IF'),
    'FOR' : Token('FOR','FOR'),
    'WHILE' : Token('WHILE','WHILE'),
    'INT' : Token('INT','INT'),
    'VEC' : Token('VEC','VEC'),
    'BOOL' : Token('BOOL','BOOL')
}

BOOLSCOPE, OTHERSCOPE = ('BOOLSCOPE', 'OTHERSCOPE')

class Token():
    def __init__(self, type, value):
        self.type = type
        self.value = value

    def __str__(self):
        return 'Token({type}, "{value}")'.format(type=self.type, value=self.value.__str__())

class Lexer():
    def __init__(self, text):
        self.text = text
        self.pos = 0
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

    def decInteger(self):
        result = ''
        while self.current_char is not None and self.current_char.isdigit():
            result += self.current_char
            self.advance()
        return int(result)

    def binInteger(self):
        result = ''
        while self.current_char is not None and self.current_char in '01':
            result += self.current_char
            self.advance()
        return int(result, 2)

    def hexInteger(self):
        result = ''
        while self.current_char is not None and (self.current_char.isdigit() or self.current_char in 'ABCDEFabcdef'):
            result += self.current_char
            self.advance()
        return int(result, 16)

    def getNextToken(self):
        while self.current_char is not None:
            if self.current_char.isspace():
                self.skipWhiteSpace()
                continue
            if self.current_char == '<' and self.peek() == '=':
                self.advance()
                self.advance()
                if self.current_scopetype == BOOLSCOPE:
                    return Token(LE, '<=')
                if self.current_scopetype == OTHERSCOPE:

class AST():
    pass

class TernaryOp(AST):
    def __init__(self, boolean, op, left, right):
        self.boolean = boolean
        self.token = self.op = op
        self.left = left
        self.right = right

class BinaryOp(AST):
    def __init__(self, left, op, right):
        self.left = left
        self.token = self.op = op
        self.right = right

class UnaryOp(AST):
    def __init__(self, op, right):
        self.token = self.op = op
        self.right = right

class Assign(AST):
    def __init__(self, left, op, right):
        self.left = left
        self.token = self.op = op
        self.right = right

class Var(AST):
    def __init__(self, token):
