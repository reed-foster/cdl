# parser.py - Reed Foster
# parser for token streams; generates an AST

from enums import *
from lexer import *
from ast import *

class Parser(object):

    def __init__(self, lexer):
        self.lexer = lexer
        self.current_token = self.lexer.getNextToken()
        # dictionaries that contain keys (name of variable/signal) and corresponding values (scope of the variable)
        varlist = {}
        siglist = {}

    def error(self):
        raise Exception('Syntax Error')

    def eat(self, token_type):
        if self.current_token.type == token_type:
            self.current_token = self.lexer.getNextToken()
        else:
            self.error()

    def term(self):
        token = self.current_token
        if token.type in (BININTCONST, HEXINTCONST, BINVECCONST, HEXVECCONST):
            return self.constant()
        if token.type == LPAREN:
            self.eat(LPAREN)
            node = self.expression()
            self.eat(RPAREN)
            return node
        if token.type == ID:
            return self.identifier()

    def power(self):
        node = self.term()
        if self.current_token.type == EXP:
            token = self.current_token
            self.eat(EXP)
            node = BinaryOp(node, token, self.term())
        return node

    def factor(self):
        token = self.current_token
        if token.type == SUB:
            self.eat(SUB)
            node = UnaryOp(token, self.power())
            return node
        if token.type == BITWISEOP and token.value == 'not':
            self.eat(BITWISEOP)
            node = UnaryOp(token, self.power())
            return node
        return self.power()

    def product(self):
        node = self.factor()
        if self.current_token.type in (MUL, DIV, MOD) or (self.current_token.type == BITWISEOP and self.current_token.value in ('and', 'nand', 'xor', 'xnor')):
            token = self.current_token
            self.eat(token.type)
            node = BinaryOp(node, token, self.factor())
        return node

    def sum(self):
        node = self.product()
        if self.current_token.type in (ADD, SUB) or (self.current_token.type == BITWISEOP and self.current_token.value in ('or', 'nor')):
            token = self.current_token
            self.eat(token.type)
            node = BinaryOp(node, token, self.product())
        return node

    def relation(self):
        token = self.current_token
        if token.type == BOOLCONST:
            self.eat(BOOLCONST)
            return token
        else:
            node = self.sum()
            if self.current_token.type in (LT, GT, LE, GE, EQ, NE):
                token = self.current_token
                self.eat(token.type)
                node = BinaryOp(node, token, self.sum())
            return node

    def boolfactor(self):
        token = self.current_token
        if token.type == NOT:
            self.eat(NOT)
            node = UnaryOp(token, self.relation())
            return node
        return self.relation()

    def boolexpr(self):
        node = self.boolfactor()
        if self.current_token.type in (AND, OR, XOR):
            token = self.current_token
            self.eat(token.type)
            node = BinaryOp(node, token, self.boolfactor())
        return node

    def expression(self):
        node = self.boolexpr()
        if self.current_token.type == TERNQ:
            self.eat(TERNQ)
            left = self.expression()
            self.eat(TERNSEP)
            node = TernaryOp(node, left, self.expression())
        return node

    def identifier(self):
        left = self.current_token
        self.eat(ID)
        token = self.current_token
        if token.type == PERIOD:
            self.eat(PERIOD)
            return BinaryOp(left, token, self.identifier())
        return Identifier(left)

    def constant(self):
        token = self.current_token
        if token.type in (BOOLCONST, BININTCONST, HEXINTCONST, DECINTCONST, BINVECCONST, HEXVECCONST):
            self.eat(token.type)
        else:
            self.eat(ID)
        return Constant(token)

    def gendeclare(self):
        gentype = self.current_token
        self.eat(TYPE)
        token = self.current_token
        self.eat(ID)
        self.eat(EOL)
        return Generic(Identifier(token), gentype)

    def sigdeclare(self):
        self.eat(SIGNAL)
        sigtype = self.current_token
        self.eat(TYPE)
        token = self.current_token
        self.eat(ID)
        self.eat(EOL)
        return Signal(Identifier(token), sigtype)

    def vardeclare(self):
        self.eat(VARIABLE)
        vartype = self.current_token
        self.eat(TYPE)
        token = self.current_token
        self.eat(ID)
        self.eat(EOL)
        return Variable(Identifier(token), vartype)

    def genericassign(self):
        item = self.current_token
        self.eat(ID)
        self.eat(ASSIGN)
        constant = Constant(self.current_token)
        self.eat(self.current_token.type)
        assignment = BinaryOp(Identifier(item), Token(ASSIGN, '='), constant)
        return assignment

    def genericlist(self):
        assignment = self.genericassign()
        if self.current_token.type == COMMA:
            token = self.current_token
            self.eat(COMMA)
            return BinaryOp(assignment, token, self.genericlist())
        return assignment

    def component(self):
        self.eat('COMPONENT')
        name = self.identifier()
        self.eat(LBRACE)
        body = ComponentBody()
        while self.current_token.type != RBRACE:
            if self.current_token.type == TYPE:
                body.children.append(self.gendeclare())
            elif self.current_token.type == 'PORT':
                # TODO: add error for multiple port declarations
                body.children.append(self.port())
            elif self.current_token.type == ID or self.current_token.type == 'ARCH':
                body.children.append(self.arch())
        self.eat(RBRACE)
        node = Component(name, body)
        return node

    def port(self):
        self.eat('PORT')
        self.eat(LBRACE)
        body = PortList()
        while self.current_token.type != RBRACE:
            direction = self.current_token
            self.eat(PORTDIR)
            porttype = self.current_token
            self.eat(TYPE)
            body.children.append(Port(Identifier(self.current_token), porttype, direction))
            self.eat(ID)
            self.eat(EOL)
        self.eat(RBRACE)
        return body

    def arch(self):
        archname = Token(ID, 'implementation')
        if self.current_token == ID:
            archname = self.current_token
            self.eat(ID)
        self.eat('ARCH')
        self.eat(LBRACE)
        body = ArchBody()
        while self.current_token.type != RBRACE:
            if self.current_token.type == SIGNAL:
                body.children.append(self.sigdeclare())
            elif self.current_token.type == 'CONNECT':
                body.children.append(self.connect())
            elif self.current_token.type == 'PROCESS':
                body.children.append(self.process())
            elif self.current_token.type == 'GENERATE':
                body.children.append(self.generate())
            elif self.current_token.type == ID:
                #need to distinguish b/w component instatiation and signal assignment
                node = self.identifier()
                if self.current_token.type == SIGASSIGN:
                    self.eat(SIGASSIGN)
                    body.children.append(BinaryOp(node, Token(SIGASSIGN, '<='), self.expression()))
                    self.eat(EOL)
                elif self.current_token.type == ID:
                    name = self.current_token
                    self.eat(ID)
                    self.eat(ASSIGN)
                    self.eat('NEW')
                    if self.current_token.type == ID and self.current_token.value == node.token.value:
                        self.eat(ID)
                        self.eat(LPAREN)
                        #parse generic assignment list
                        generics = self.genericlist()
                        self.eat(RPAREN)
                        body.children.append(CompInst(Identifier(name), node, generics))
                        self.eat(EOL)
                    else:
                        self.error()
        self.eat(RBRACE)
        node = Arch(Identifier(archname), body)
        return node


lex = Lexer(
'''
component CompName
{
    int genericint;
    bool genericbool;
    vec genericvec;

    port
    {
        input int inputint;
        input vec inputvec;
        output bool outputbool;
    }

    arch
    {
        signal vec foo;
        foo <= fox < banana;
        CompType compinst = new CompType(lol = 3, foo = 5, banana = x"4");
    }
}
''')
parse = Parser(lex)

tree = parse.component()

print tree