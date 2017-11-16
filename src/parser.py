class Parser():

    def __init__(self, lexer):
        self.lexer = lexer
        self.current_token = self.lexer.getNextToken()

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