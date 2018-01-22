# lex.py - Reed Foster
# Software for automated generation of a lexer and parser from grammar

# Enumerations for tokens
NONTERMINAL, TERMINAL = ('NONTERMINAL', 'TERMINAL')
ASSIGN, COMMA, LBRACE, RBRACE, LPAREN, RPAREN, PIPE, LBRACKET, RBRACKET, EOL = ('ASSIGN', 'COMMA', 'LBRACE', 'RBRACE', 'LPAREN', 'RPAREN', 'PIPE', 'LBRACKET', 'RBRACKET', 'EOL')
EOF = 'EOF'


class Token(object):
    def __init__(self, type, value):
        self.type = type
        self.value = value

    def __str__(self):
        return 'Token({type}, "{value}")'.format(type=self.type, value=self.value.__str__())



class Lexer(object):
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
        peekpos = self.pos + 1
        if peekpos > len(self.text) - 1:
            return None
        else:
            return self.text[self.pos]

    def skipWhiteSpace(self):
        while self.current_char is not None and self.current_char.isspace():
            self.advance()

    def comment(self):
        while self.current_char is not None and (not (self.current_char == '*' and self.peek() == ')')):
            self.advance()
        self.advance()
        self.advance()
        self.skipWhiteSpace()

    def terminal(self):
        result = ''
        if self.current_char == '"':
            token = self.string()
        else:
            while self.current_char is not None and self.current_char.isupper():
                result += self.current_char
                self.advance()
            token = Token(TERMINAL, result)
        return token

    def nonterminal(self):
        result = ''
        while self.current_char is not None and (self.current_char.islower() or self.current_char == '_'):
            result += self.current_char
            self.advance()
        token = Token(NONTERMINAL, result)
        return token

    def string(self):
        result = ''
        while self.current_char is not None and self.current_char != '"':
            result += self.current_char
            self.advance()
        self.advance()
        token = Token(TERMINAL, result + '"')
        return token

    def getNextToken(self):
        while self.current_char is not None:
            self.skipWhiteSpace()
            if self.current_char == '(' and self.peek() == '*':
                self.comment()

            # Tokenize operators
            # ASSIGN, COMMA, LBRACE, RBRACE, LPAREN, RPAREN, PIPE, LBRACKET, RBRACKET, EOL
            if self.current_char == '=':
                self.advance()
                return Token(ASSIGN, '=')
            if self.current_char == ',':
                self.advance()
                return Token(COMMA, ',')
            if self.current_char == '{':
                self.advance()
                return Token(LBRACE, '{')
            if self.current_char == '}':
                self.advance()
                return Token(RBRACE, '}')
            if self.current_char == '(':
                self.advance()
                return Token(LPAREN, '(')
            if self.current_char == ')':
                self.advance()
                return Token(RPAREN, ')')
            if self.current_char == '[':
                self.advance()
                return Token(LBRACKET, '[')
            if self.current_char == ']':
                self.advance()
                return Token(RBRACKET, ']')
            if self.current_char == '|':
                self.advance()
                return Token(PIPE, '|')
            if self.current_char == ';':
                self.advance()
                return Token(EOL, ';')

            if self.current_char == '"' or self.current_char.isupper():
                return self.terminal()
            if self.current_char.islower():
                return self.nonterminal()

            self.error()
        return Token(EOF, None)




class AST(object):
    pass

class BinaryOp(AST):
    def __init__(self, left, op, right):
        self.left = left
        self.token = self.op = op
        self.right = right

class UnaryOp(AST):
    def __init__(self, op, operand):
        self.token = self.op = op
        self.operand = operand

class Terminal(AST):
    def __init__(self, token):
        self.token = token

class Nonterminal(AST):
    def __init__(self, token):
        self.token = token



class Parser(object):
    def __init__(self, lexer):
        self.lexer = lexer
        self.current_token = self.lexer.getNextToken()
        self.grammar = []

    def error(self):
        raise Exception('Syntax Error')

    def eat(self, type):
        if self.current_token.type == type:
            self.current_token = self.lexer.getNextToken()
        else:
            self.error()

    def parse(self):
        while self.current_token.type != EOF:
            self.grammar.append(self.rule())

    def rule(self):
        left = self.current_token
        self.eat(NONTERMINAL)
        op = self.current_token
        self.eat(ASSIGN)
        node = BinaryOp(left, op, self.expression())
        self.eat(EOL)
        return node

    def expression(self):
        node = self.concatenation()
        if self.current_token.type == PIPE:
            token = self.current_token
            self.eat(token.type)
            node = BinaryOp(node, token, self.expression())
        return node

    def concatenation(self):
        node = self.factor()
        if self.current_token.type == COMMA:
            token = self.current_token
            self.eat(token.type)
            node = BinaryOp(node, token, self.concatenation())
        return node

    def factor(self):
        token = self.current_token
        if token.type == LBRACE:
            self.eat(LBRACE)
            node = UnaryOp(Token('REP', '{{}}'), self.expression())
            self.eat(RBRACE)
            return node
        if token.type == LBRACKET:
            self.eat(LBRACKET)
            node = UnaryOp(Token('OPT', '[]'), self.expression())
            self.eat(RBRACKET)
            return node
        if token.type == LPAREN:
            self.eat(LPAREN)
            node = UnaryOp(Token('GRP', '()'), self.expression())
            self.eat(RPAREN)
            return node
        if token.type == TERMINAL:
            self.eat(TERMINAL)
            return Terminal(token)
        if token.type == NONTERMINAL:
            self.eat(NONTERMINAL)
            return Nonterminal(token)

lex = Lexer (
    '''expression = (boolexpr, TERNQ, expression, TERNSEP, expression) | sum;
    boolexpr = boolfactor, [boolop, boolexpr];
    boolfactor = [NOT], (relation | BOOLCONST | identifier);'''
)

parse = Parser(lex)

parse.parse()

