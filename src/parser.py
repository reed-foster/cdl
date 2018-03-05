# parser.py - Reed Foster
# parser for token streams; generates an AST

import lexer
import ast
from enums import *

class Component(object):
    def __init__(self, siglist = {}, varlist = {}, portlist = {}, genlist = {}, tree = ast.AST()):
        self.siglist = siglist
        self.varlist = varlist
        self.portlist = portlist
        self.complist = complist
        self.tree = tree

class Parser(object):

    def __init__(self, lexer):
        self.lexer = lexer
        self.current_token = self.lexer.getNextToken()
        self.component = Component()
        self.complist = {}
        self.veclist = {} # list of all identifiers (signals, variables, or generics) that are type vector

    # wrapper for component method
    def parse(self):
        while self.current_token.type == 'COMPONENT':
            comp = self.component()
            self.complist[comp.name.token.value] = comp
        return self.complist

    def error(self):
        raise Exception('Syntax Error')

    def eat(self, token_type):
        if self.current_token.type == token_type:
            self.current_token = self.lexer.getNextToken()
        else:
            self.error()


    # Expression parsing and utilities
    def expression(self):
        node = self.boolexpr()
        while self.current_token.type == QUESTION:
            self.eat(QUESTION)
            left = self.expression()
            self.eat(COLON)
            node = ast.TernaryOp(node, left, self.expression())
        return node

    def boolexpr(self):
        node = self.boolfactor()
        while self.current_token.type in (AND, OR, XOR):
            token = self.current_token
            self.eat(token.type)
            node = ast.BinaryOp(node, token, self.boolfactor())
        return node

    def boolfactor(self):
        token = self.current_token
        if token.type == NOT:
            self.eat(NOT)
            node = ast.UnaryOp(token, self.relation())
            return node
        return self.relation()

    def relation(self):
        token = self.current_token
        if token.type == BOOLCONST:
            self.eat(BOOLCONST)
            return ast.Identifier(token)
        else:
            self.lexer.current_scopetype = BOOLSCOPE
            node = self.sum()
            if self.current_token.type in (LT, GT, LE, GE, EQ, NE):
                token = self.current_token
                self.eat(token.type)
                node = ast.BinaryOp(node, token, self.sum())
            self.lexer.current_scopetype = OTHERSCOPE
            return node

    # Parse arithmetic summation operations and boolean summation operations
    def sum(self):
        node = self.product()
        while self.current_token.type in (ADD, SUB) or (self.current_token.type == BITWISEOP and self.current_token.value in ('or', 'nor')) or self.current_token.type == CONCAT:
            token = self.current_token
            self.eat(token.type)
            node = ast.BinaryOp(node, token, self.product())
        return node

    # Parse arithmetic multiplication operations and boolean multiplication operations
    def product(self):
        node = self.factor()
        while self.current_token.type in (MUL, DIV, MOD) or (self.current_token.type == BITWISEOP and self.current_token.value in ('and', 'nand', 'xor', 'xnor')):
            token = self.current_token
            self.eat(token.type)
            node = ast.BinaryOp(node, token, self.factor())
        return node

    def factor(self):
        token = self.current_token
        if token.type == SUB:
            self.eat(SUB)
            node = ast.UnaryOp(token, self.power())
            return node
        if token.type == BITWISEOP and token.value == 'not':
            self.eat(BITWISEOP)
            node = ast.UnaryOp(token, self.power())
            return node
        return self.power()

    def power(self):
        node = self.term()
        while self.current_token.type == EXP:
            token = self.current_token
            self.eat(EXP)
            node = ast.BinaryOp(node, token, self.term())
        return node

    def term(self):
        token = self.current_token
        if token.type in (BININTCONST, HEXINTCONST, DECINTCONST, BINVECCONST, HEXVECCONST):
            return self.constant()
        if token.type == LPAREN:
            self.eat(LPAREN)
            node = ast.UnaryOp(token.Token('PAREN', '()'), self.expression())
            self.eat(RPAREN)
            return node
        if token.type == ID:
            identifier = self.identifier()
            return identifier

    # helper method for identifier parsing; if a splice of a vector is detected, then use getsplice to parse it
    def getsplice(self, identifier):
        if identifier.value not in self.veclist: # works for now, but if identifier is a compound id (i.e. a subcomponent port), then this fails
            raise Exception('Type Error: ' + identifier.value + ' is not of type VEC')
        self.eat(LBRACKET)
        top = self.expression()
        if self.current_token.type == RBRACKET:
            bottom = top
        else:
            self.eat(COLON)
            bottom = self.expression()
        self.eat(RBRACKET)
        return ast.Splice(ast.Identifier(identifier), top, bottom)


    # Parsing of composite tokens
    def identifier(self):
        left = self.current_token
        self.eat(ID)
        token = self.current_token
        if token.type == LBRACKET:
            return self.getsplice(left)
        if token.type == PERIOD:
            self.eat(PERIOD)
            right = self.current_token
            self.eat(ID)
            node = ast.BinaryOp(ast.Identifier(left), token, ast.Identifier(right))
            if token.type == LBRACKET:
                return self.getsplice(node)
            return node
        return ast.Identifier(left)

    def constant(self):
        token = self.current_token
        if token.type in (BOOLCONST, BININTCONST, HEXINTCONST, DECINTCONST, BINVECCONST, HEXVECCONST):
            self.eat(token.type)
        else:
            self.eat(ID)
        return ast.Constant(token)

    # Returns the width of the vector (in bits)
    def getvecwidth(self):
        if self.current_token.type == LBRACKET:
            self.eat(LBRACKET)
            width = self.current_token.value
            self.eat(DECINTCONST)
            self.eat(RBRACKET)
            return width
        return '1'

    # Parsing of declarations
    def gendeclare(self):
        gentype = self.current_token
        self.eat(TYPE)
        token = self.current_token
        self.eat(ID)
        width = None
        if gentype.value == 'vec':
            self.veclist[token.value] = ''
            width = self.getvecwidth()
        self.eat(EOL)
        return ast.Generic(ast.Identifier(token), gentype, width)

    def sigdeclare(self):
        self.eat(SIGNAL)
        sigtype = self.current_token
        self.eat(TYPE)
        token = self.current_token
        self.eat(ID)
        width = None
        if sigtype.value == 'vec':
            self.veclist[token.value] = ''
            width = self.getvecwidth()
        self.eat(EOL)
        return ast.Signal(ast.Identifier(token), sigtype, width)

    def vardeclare(self):
        self.eat(VARIABLE)
        vartype = self.current_token
        self.eat(TYPE)
        token = self.current_token
        self.eat(ID)
        width = None
        if vartype.value == 'vec':
            self.veclist[token.value] = ''
            width = self.getvecwidth()
        self.eat(EOL)
        return ast.Variable(ast.Identifier(token), vartype, width)

    # Parse entire component
    def component(self):
        self.eat('COMPONENT')
        name = self.identifier()
        self.eat(LBRACE)
        body = ast.ComponentBody()
        while self.current_token.type != RBRACE:
            if self.current_token.type == TYPE:
                body.children.append(self.gendeclare())
            elif self.current_token.type == 'PORT':
                # TODO: add error for multiple port declarations
                body.children.append(self.port())
            elif self.current_token.type == ID or self.current_token.type == 'ARCH':
                body.children.append(self.arch())
        self.eat(RBRACE)
        node = ast.Component(name, body)
        return node

    # Parse port declaration
    def port(self):
        self.eat('PORT')
        self.eat(LBRACE)
        body = ast.PortList()
        while self.current_token.type != RBRACE:
            direction = self.current_token
            self.eat(PORTDIR)
            porttype = self.current_token
            self.eat(TYPE)
            name = self.current_token
            self.eat(ID)
            width = self.getvecwidth() if porttype.value == 'vec' else None
            self.eat(EOL)
            body.children.append(ast.Port(ast.Identifier(name), porttype, direction, width))
        self.eat(RBRACE)
        return body

    # Parse architecture
    def arch(self):
        archname = token.Token(ID, 'implementation')
        if self.current_token.type == ID:
            archname = self.current_token
            self.eat(ID)
        self.eat('ARCH')
        self.eat(LBRACE)
        body = ast.ArchBody()
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
                    body.children.append(ast.BinaryOp(node, token.Token(SIGASSIGN, '<='), self.expression()))
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
                        if self.current_token.type != RPAREN:
                            generics = self.genericlist()
                        else:
                            generics = None
                        self.eat(RPAREN)
                        body.children.append(ast.CompInst(ast.Identifier(name), node, generics))
                        self.eat(EOL)
                    else:
                        self.error()
        self.eat(RBRACE)
        node = ast.Arch(ast.Identifier(archname), body)
        return node


    # Parse generic assignments for component instantiation
    def genericlist(self):
        assignment = self.genericassign()
        if self.current_token.type == COMMA:
            token = self.current_token
            self.eat(COMMA)
            return ast.BinaryOp(assignment, token, self.genericlist())
        return assignment

    def genericassign(self):
        item = self.current_token
        self.eat(ID)
        self.eat(ASSIGN)
        constant = ast.Constant(self.current_token)
        self.eat(self.current_token.type)
        assignment = ast.BinaryOp(ast.Identifier(item), token.Token(ASSIGN, '='), constant)
        return assignment


def test():
    lex = lexer.Lexer(
    '''
    component AndGate
    {
        int width;
        int ports;
        port
        {
            input vec inputint;
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

#test()