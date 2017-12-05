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

class CompInst(AST):
    def __init__(self, token, generics):
        self.token = token
        self.generics = generics

class Signal(AST):
    def __init__(self, token):
        self.token = token
        self.name = token.value

class Variable(AST):
    def __init__(self, token):
        self.token = token
        self.name = token.value

class Generic(AST):
    def __init__(self, token, type):
        self.token = token
        self.name = token.value
        self.type = type

class PortList(AST):
    def __init__(self):
        self.children = []

class Port(AST):
    def __init__(self, token, type, direction):
        self.token = token
        self.name = token.value
        self.type = type
        self.direction = direction

class Component(AST):
    def __init__(self, name, body):
        self.name = name
        self.body = body

class ComponentBody(AST):
    def __init__(self):
        self.children = []

class Arch(AST):
    def __init__(self, optname, body):
        if optname is not None:
            self.name = optname
        self.body = body

class ArchBody(AST):
    def __init__(self):
        self.children = []