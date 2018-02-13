# lexer.py - Reed Foster
# tokenizer/lexer for CDL

from enums import *

class Lexer(object):
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
        while self.current_char is not None and (self.current_char.isalnum() or self.current_char == '_'):
            result += self.current_char
            self.advance()
        identifier = RESERVED_KEYWORDS.get(result, token.Token(ID, result)) #dict.get(key[, default]) returns default if key isn't found
        return identifier

    def comment(self):
        while self.current_char is not None and self.current_char != '\n':
            self.advance()

    def multilinecomment(self):
        while self.current_char is not None and (not (self.current_char == '*' and self.peek() == '/')):
            self.advance()

    def getNextToken(self):
        while self.current_char is not None:

            if self.current_char.isspace():
                self.skipWhiteSpace()
                continue

            # Skip multiline comments
            if self.current_char == '/' and self.peek() == '*':
                self.multilinecomment()
                self.advance()
                self.advance()
                self.skipWhiteSpace()
                continue

            # Skip single-line comments
            if self.current_char == '/' and self.peek() == '/':
                self.comment()
                self.skipWhiteSpace()
                continue
            
            if self.current_char == ';':
                self.advance()
                return token.Token(EOL, ';')

            if self.current_char == ',':
                self.advance()
                return token.Token(COMMA, ',')
            if self.current_char == '.':
                self.advance()
                return token.Token(PERIOD, '.')

            # Tokenize hex constants
            if self.current_char == 'x' and self.peek() == '"':
                self.advance()
                self.advance()
                return token.Token(HEXVECCONST, self.hexVector())

            if self.current_char == '"':
                self.advance()
                return token.Token(BINVECCONST, self.binVector())

            if self.current_char.isalpha():
                return self.id()

            if self.current_char.isdigit():
                if self.current_char == '0' and self.peek() == 'x':
                    self.advance()
                    self.advance()
                    return token.Token(HEXINTCONST, self.hexInteger())
                if self.current_char == '0' and self.peek() == 'b':
                    self.advance()
                    self.advance()
                    return token.Token(BININTCONST, self.binInteger())
                return token.Token(DECINTCONST, self.decInteger())

            # Tokenizing '<=' based on context (either relational operator - boolean context, or signal assignment - all other contexts)
            if self.current_char == '<' and self.peek() == '=':
                self.advance()
                self.advance()
                if self.current_scopetype == BOOLSCOPE:
                    return token.Token(LE, '<=')
                if self.current_scopetype == OTHERSCOPE:
                    return token.Token(SIGASSIGN, '<=')

            # Tokenize other assignment operators
            if self.current_char == ':' and self.peek() == '=':
                self.advance()
                self.advance()
                return token.Token(VARASSIGN, ':=')
            if self.current_char == '=':
                self.advance()
                return token.Token(ASSIGN, '=')

            if self.current_char == '?':
                self.advance()
                return token.Token(TERNQ, '?')
            if self.current_char == ':':
                self.advance()
                if self.current_scopetype == IDSCOPE:
                    return token.Token(DOWNTO, ':')
                if self.current_scopetype == OTHERSCOPE:
                    return token.Token(TERNSEP, ':')

            # Tokenize relational operators
            if self.current_char == '>' and self.peek() == '=':
                self.advance()
                self.advance()
                return token.Token(GE, '>=')
            if self.current_char == '<':
                self.advance()
                return token.Token(LT, '<')
            if self.current_char == '>':
                self.advance()
                return token.Token(GT, '>')
            if self.current_char == '=' and self.peek() == '=':
                self.advance()
                self.advance()
                return token.Token(EQ, '==')
            if self.current_char == '!' and self.peek() == '=':
                self.advance()
                self.advance()
                return token.Token(NE, '!=')

            # Tokenize arithmetic operators
            if self.current_char == '+':
                self.advance()
                return token.Token(ADD, '+')
            if self.current_char == '-':
                self.advance()
                return token.Token(SUB, '-')
            if self.current_char == '*' and self.peek() == '*':
                self.advance()
                self.advance()
                return token.Token(EXP, '**')
            if self.current_char == '*':
                self.advance()
                return token.Token(MUL, '*')
            if self.current_char == '/':
                self.advance()
                return token.Token(DIV, '/')
            if self.current_char == '%':
                self.advance()
                return token.Token(MOD, '%')     

            # Tokenize boolean operators
            if self.current_char == '&':
                self.advance()
                return token.Token(AND, '&')
            if self.current_char == '|':
                self.advance()
                return token.Token(OR, '|')
            if self.current_char == '^':
                self.advance()
                return token.Token(XOR, '^')
            if self.current_char == '!':
                self.advance()
                return token.Token(NOT, '!')

            # Tokenize braces/parentheses/brackets
            if self.current_char == '(':
                self.advance()
                return token.Token(LPAREN, '(')
            if self.current_char == ')':
                self.advance()
                return token.Token(RPAREN, ')')
            if self.current_char == '{':
                self.advance()
                return token.Token(LBRACE, '{')
            if self.current_char == '}':
                self.advance()
                return token.Token(RBRACE, '}')
            if self.current_char == '[':
                self.advance()
                return token.Token(LBRACKET, '[')
            if self.current_char == ']':
                self.advance()
                return token.Token(RBRACKET, ']')

            if self.current_char == '&':
                self.advance()
                return token.Token(CONCAT, '&')

            self.error()

        return token.Token(EOF, None)

def test():
    lex = Lexer('hi <= x"12ffab"; lol := ( 0b1011); {banana <= true}; five or x"10f" := hi;')
    for i in range(23):
        print(lex.getNextToken())

#test()