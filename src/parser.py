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

    def factor(self):
        token = self.current_token
        if token.type == SUB:
            self.eat(SUB)
            node = UnaryOp(token, self.factor())
            return node
        if token.type in (INT, VEC, BOOL):
            return token
        if token.type == LPAREN:
            self.eat(LPAREN)
            node = self.expression()
            self.eat(RPAREN)
            return nodes

    def power(self):
        node = self.factor()
        while self.current_token.type == EXP:
            token = self.current_token
            self.eat(EXP)
            node = BinaryOp(node, token, self.factor())
        return node

    def term(self):
        node = self.power()
        while self.current_token.type in (MUL, DIV, MOD):
            token = self.current_token
            self.eat(token.type)
            node = BinaryOp(node, token, self.power())
        return node
            

    def expression(self):
        node = self.term()
        while self.current_token.type in (ADD, SUB, TENRQ):
            token = self.current_token
            if token.type == TERNQ:
                self.eat(TERNQ)
                left = self.expression()
                self.eat(TERNSEP)
                node = TernaryOp
            self.eat(token.type)
            node = BinaryOp(node, token, self.term())
        return node

    def relationalexpression(self):
        node = self.expression()
        while self.current_token.type in (LT, GT, LE, GE, EQ, NE):
            token = self.current_token
            self.eat(token.type)
            node = BinaryOp(node, token, self.term())
        return node

    def booleanfactor(self):
        node = self.relationalexpression()
        if self.current_token.type == NOT:
            self.eat(NOT)
            node = UnaryOp(token, self.relationalexpression())
            return node
        #if self.current_token.type
    
    def gendeclare(self):
        self.eat(TYPE)
        token = self.current_token
        

    def sigdeclare(self):
        self.eat(SIGNAL)
        self.eat(TYPE)
        token = self.current_token


    def vardeclare(self):

    def sigassign(self):
        left = self.signal()
        token = self.current_token
        self.eat(SIGASSIGN)

    def component(self):
        self.eat('COMPONENT')
        name = self.current_token
        self.eat(ID)
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
            elif self.current_token.type == ID:
                #need to distinguish b/w component instatiation and signal assignment
                token = self.current_token
                self.eat(ID)
                if self.current_token.type == SIGASSIGN:
                    self.eat(SIGASSIGN)
                    body.children.append(Assign(token, self.expression()))
                elif self.current_token.type == ID:
                    name = self.current_token
                    self.eat(ID)
                    self.eat(COMPASSIGN)
                    self.eat('NEW')
                    if self.current_token.type == ID and self.current_token.value == token.value:
                        self.eat(ID)
                        self.eat(LPAREN)
                        #parse generic assignment list
                        generics = 
                        self.eat(RPAREN)
                        body.children.append(CompInst(name, generics))
                    else:
                        self.error()
            elif self.current_token.type == 'CONNECT':
                body.children.append(self.connect())
            elif self.current_token.type == 'PROCESS':
                body.children.append(self.process())
            elif self.current_token.type == 'GENERATE':
                body.children.append(self.generate())
        self.eat(RBRACE)
        node = Arch(name, body)
        return node