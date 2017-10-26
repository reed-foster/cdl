ID, SIGASSIGN, VARASSIGN = ('ID', 'SIGASSIGN', 'VARASSIGN')
INT, VEC, BOOL = ('INT', 'VEC', 'BOOL')
ADD, SUB, MUL, DIV, MOD, EXP = ('ADD', 'SUB', 'MUL', 'DIV', 'MOD', 'EXP')
BOOLEANAND, BOOLEANOR, BOOLEANXOR, BOOLEANNOT = ('BOOLEANAND', 'BOOLEANOR', 'BOOLEANXOR', 'BOOLEANNOT')
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
    'BOOL' : Token('BOOL','BOOL'),
    'TRUE' : Token('TRUE', 'TRUE'),
    'FALSE' : Token('FALSE', 'FALSE'),
    'AND' : Token('AND', 'AND'),
    'OR' : Token('OR', 'OR'),
    'NOT' : Token('NOT', 'NOT'),
    'NAND' : Token('NAND', 'NAND'),
    'NOR' : Token('NOR', 'NOR'),
    'XOR' : Token('XOR', 'XOR'),
    'XNOR' : Token('XNOR', 'XNOR'),
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
        return result

    def binVector(self):
        result = ''
        while self.current_char is not None and self.current_char in '01':
            result += self.current_char
            self.advance()
        return result

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

            if self.current_char == '/' and self.peek() == '/':
                return self.comment()

            if self.current_char == 'x' and self.peek() == '"':
                self.advance()
                self.advance()
                return Token(VEC, self.hexVector())

            if self.current_char == '"':
                self.advance()
                return Token(VEC, self.binVector())

            if self.current_char.isalpha():
                return self.id()

            if self.current_char.isdigit():
                if self.current_char == '0' and self.peek() == 'x':
                    return Token(INT, self.hexInteger())
                if self.current_char == '0' and self.peek() == 'b':
                    return Token(INT, self.binInteger())
                return Token(INT, self.decInteger())

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
            if self.current_char == '*' and self.peek() = '*':
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

            self.error()

        return Token(EOF, None)

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


class Parser():

    def __init__(self, lexer):
        self.lexer = lexer
        self.current_token = self.lexer.getNextToken()

    def error(self):
        raise Exception('Syntax Error')

    def eat(self, token_type):
        if self.current_token.type = token_type:
            self.current_token = self.lexer.getNextToken()
        else:
            self.error()

    def factor(self):
        token = self.current_token
        if token.type == SUB:
            self.eat(SUB)
            node = UnaryOp(token, self.factor())
            return node
        if token.type in (INT, VEC, BOOL):
            