# enums.py - Reed Foster
# String enumerations for various keywords

import token

ID, SIGASSIGN, VARASSIGN, ASSIGN = ('ID', 'SIGASSIGN', 'VARASSIGN', 'ASSIGN')
TYPE, BITWISEOP, ARCHTYPE, PORTDIR = ('TYPE', 'BITWISEOP', 'ARCHTYPE', 'PORTDIR')
SIGNAL, VARIABLE = ('SIGNAL', 'VARIABLE')
DECINTCONST, BININTCONST, HEXINTCONST, BINVECCONST, HEXVECCONST, BOOLCONST = ('DECINTCONST', 'BININTCONST', 'HEXINTCONST', 'BINVECCONST', 'HEXVECCONST', 'BOOLCONST')
ADD, SUB, MUL, DIV, MOD, EXP = ('ADD', 'SUB', 'MUL', 'DIV', 'MOD', 'EXP')
AND, OR, XOR, NOT = ('AND', 'OR', 'XOR', 'NOT')
CONCAT, COLON = ('CONCAT', 'COLON')
LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET = ('LPAREN', 'RPAREN', 'LBRACE', 'RBRACE', 'LBRACKET', 'RBRACKET')
QUESTION = 'QUESTION'
LT, GT, LE, GE, EQ, NE = ('LT', 'GT', 'LE', 'GE', 'EQ', 'NE')
EOF, EOL = ('EOF', 'EOL')
COMMA, PERIOD = ('COMMA', 'PERIOD')

BOOLSCOPE, IDSCOPE, OTHERSCOPE = ('BOOLSCOPE', 'IDSCOPE', 'OTHERSCOPE')

RESERVED_KEYWORDS = {
    'component' : token.Token('COMPONENT', 'component'),
    'port' : token.Token('PORT','port'),
    'arch' : token.Token('ARCH','arch'),
    'generate' : token.Token('GENERATE','generate'),
    'process' : token.Token('PROCESS', 'process'),
    'signal' : token.Token(SIGNAL,'signal'),
    'variable' : token.Token(VARIABLE,'variable'),
    'new' : token.Token('NEW','new'),
    'if' : token.Token('IF','if'),
    'else' : token.Token('ELSE', 'else'),
    'for' : token.Token('FOR','for'),
    'while' : token.Token('WHILE','while'),
    'input' : token.Token(PORTDIR, 'input'),
    'output' : token.Token(PORTDIR, 'output'),
    'int' : token.Token(TYPE,'int'),
    'uint' : token.Token(TYPE, 'uint'),
    'vec' : token.Token(TYPE,'vec'),
    'bool' : token.Token(TYPE,'bool'),
    'true' : token.Token(BOOLCONST, 'true'),
    'false' : token.Token(BOOLCONST, 'false'),
    'and' : token.Token(BITWISEOP, 'and'),
    'or' : token.Token(BITWISEOP, 'or'),
    'not' : token.Token(BITWISEOP, 'not'),
    'nand' : token.Token(BITWISEOP, 'nand'),
    'nor' : token.Token(BITWISEOP, 'nor'),
    'xor' : token.Token(BITWISEOP, 'xor'),
    'xnor' : token.Token(BITWISEOP, 'xnor'),
    'implementation' : token.Token(ARCHTYPE, 'implementation'),
    'verification' : token.Token(ARCHTYPE, 'verification')
}