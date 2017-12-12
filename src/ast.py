# ast.py - Reed Foster
# Classes for all AST nodes

class AST():
    pass

class TernaryOp(AST):
    def __init__(self, boolean, left, right):
        self.boolean = boolean
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

class CompInst(AST):
    def __init__(self, token, comptype, generics):
        self.token = token
        self.generics = generics
        self.type = comptype

class Signal(AST):
    def __init__(self, token, sigtype):
        self.token = token
        self.name = token.value
        self.type = sigtype

class Variable(AST):
    def __init__(self, token, vartype):
        self.token = token
        self.name = token.value
        self.type = vartype

class Generic(AST):
    def __init__(self, token, gentype):
        self.token = token
        self.name = token.value
        self.type = gentype

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