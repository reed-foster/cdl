# ast.py - Reed Foster
# Classes for all AST nodes

class AST(object):
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
    def __init__(self, name, comptype, generics):
        self.name = name
        self.generics = generics
        self.comptype = comptype

class Signal(AST):
    def __init__(self, name, sigtype, width = None):
        self.name = name
        self.sigtype = sigtype
        self.width = width

class Variable(AST):
    def __init__(self, name, vartype, width = None):
        self.name = name
        self.vartype = vartype
        self.width = width

class Generic(AST):
    def __init__(self, name, gentype, width = None):
        self.name = name
        self.gentype = gentype
        self.width = width

class Constant(AST):
    def __init__(self, token):
        self.token = token

class Identifier(AST):
    def __init__(self, token):
        self.token = token

class PortList(AST):
    def __init__(self):
        self.children = []

class Port(AST):
    def __init__(self, name, porttype, direction, width = None):
        self.name = name
        self.porttype = porttype
        self.direction = direction
        self.width = width

class Component(AST):
    def __init__(self, name, body):
        self.name = name
        self.body = body

class ComponentBody(AST):
    def __init__(self):
        self.children = []

class Arch(AST):
    def __init__(self, optname, body):
        self.name = optname
        self.body = body

class ArchBody(AST):
    def __init__(self):
        self.children = []