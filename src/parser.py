# parser.py - Reed Foster
# parser for token streams; generates an AST

class Parser():

    def __init__(self, lexer):
        self.lexer = lexer
        self.current_token = self.lexer.getNextToken()
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
            if self.current_token in (LT, GT, LE, GE, EQ, NE):
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
        return left

    def constant(self):
        token = self.current_token
        if token.type in (BOOLCONST, BININTCONST, HEXINTCONST, BINVECCONST, HEXVECCONST):
            self.eat(token.type)
        else:
            self.eat(ID)
        return token

    def gendeclare(self):
        gentype = self.current_token
        self.eat(TYPE)
        token = self.current_token
        self.eat(ID)
        self.eat(EOL)
        return Generic(token, gentype)

    def sigdeclare(self):
        self.eat(SIGNAL)
        sigtype = self.current_token
        self.eat(TYPE)
        token = self.current_token
        self.eat(ID)
        self.eat(EOL)
        return Signal(token, sigtype)

    def vardeclare(self):
        self.eat(VARIABLE)
        vartype = self.current_token
        self.eat(TYPE)
        token = self.current_token
        self.eat(ID)
        self.eat(EOL)
        return Variable(token, vartype)

    def genericlist(self):
        item = self.current_token
        if item.type != ID:
            assignment = self.constant()
        else:
            self.eat(ID)
            self.eat(ASSIGN)
            assignment = BinaryOp(item, Token(ASSIGN, '='), self.constant())
        if self.current_token.type == ',':
            token = self.current_token
            self.eat(COMMA)
            return BinaryOp(assignment, token, self.genericlist())
        return item

    def component(self):
        self.eat('COMPONENT')
        name = self.identifier()
        self.eat(LBRACE)
        body = ComponentBody()
        while self.current_token.type != RBRACE:
            if self.current_token.type == TYPE:
                body.children.append(self.generic())
            elif self.current_token.type == 'PORT':
                # TODO: add error for multiple port declarations
                body.children.append(self.port())
            elif self.current_token.type == ID || self.current_token.type == 'ARCH':
                body.children.append(self.arch())
        self.eat(RBRACE)
        node = Component(name, body)
        return node

    def generic(self):
        type = self.current_token
        self.eat(TYPE)
        node = Generic(self.current_token, type)
        self.eat(ID)
        self.eat(EOL)
        return node

    def port(self):
        self.eat('PORT')
        self.eat(LBRACE)
        body = PortList()
        while self.current_token.type != RBRACE:
            direction = self.current_token
            self.eat(PORTDIR)
            type = self.current_token
            self.eat(TYPE)
            body.children.append(Port(self.current_token, type, direction))
            self.eat(ID)
            self.eat(EOL)
        self.eat(RBRACE)
        return body

    def arch(self):
        name = None
        if self.current_token == ID:
            name = self.current_token
            self.eat(ID)
        self.eat('ARCH')
        self.eat(LBRACE)
        body = ArchBody()
        while self.current_token.type != RBRACE:
            if self.current_token.type == SIGNAL:
                body.children.append(self.signaldeclaration())
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
                    if self.current_token.type == ID and self.current_token.value == node.value:
                        self.eat(ID)
                        self.eat(LPAREN)
                        #parse generic assignment list
                        generics = self.genericlist()
                        self.eat(RPAREN)
                        body.children.append(CompInst(name, generics))
                        self.eat(EOL)
                    else:
                        self.error()
        self.eat(RBRACE)
        node = Arch(name, body)
        return node