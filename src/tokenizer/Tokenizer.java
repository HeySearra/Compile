package tokenizer;

import error.TokenizeError;

import java.util.regex.Pattern;

import error.ErrorCode;
import util.Pos;
import tokenizer.StringIter;
import tokenizer.Token;

public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了
    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();

        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        if (Character.isDigit(peek)) {
            return lexUInt();
        } else if (Character.isAlphabetic(peek)) {
            return lexIdentOrKeyword();
        } else {
            return lexOperatorOrUnknown();
        }
    }

    private Token lexUInt() throws TokenizeError {
        Pos startPos;
        try{
            startPos = it.currentPos();
        }catch(Error e){
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (it.isEOF()){
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (!Character.isDigit(it.peekChar())) {
            throw new TokenizeError(ErrorCode.InvalidInput, it.currentPos());
        }
        StringBuffer uint=new StringBuffer("");
        while(!it.isEOF() && Character.isDigit(it.peekChar())){
            uint.append(it.nextChar());
        }
        int num;
        try{
            num = Integer.parseInt(uint.toString());
        }catch(Exception e){
            throw new TokenizeError(ErrorCode.InvalidPrint, it.currentPos());
        }
        if (num > 2147483647){
            throw new TokenizeError(ErrorCode.IntegerOverflow, it.currentPos());
        }
        Token token = new Token(TokenType.UINT_LITERAL, num, startPos, it.currentPos());
        return token;
    }

    private Token lexIdentOrKeyword() throws TokenizeError {
        StringBuffer b = new StringBuffer("");
        Pos startPos;
        try{
            startPos = it.currentPos();
        }catch(Error e){
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (it.isEOF()){
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (!Character.isLetter(it.peekChar())) {
            throw new TokenizeError(ErrorCode.InvalidInput, it.currentPos());
        }
        while (!it.isEOF() && Character.isLetterOrDigit(it.peekChar())) {
            b.append(it.nextChar());
        }
        String s = b.toString();
        switch (s) {
            case "fn":
                return new Token(TokenType.FN_KW, s, startPos, it.currentPos());
            case "let":
                return new Token(TokenType.LET_KW, s, startPos, it.currentPos());
            case "const":
                return new Token(TokenType.CONST_KW, s, startPos, it.currentPos());
            case "as":
                return new Token(TokenType.AS_KW, s, startPos, it.currentPos());
            case "while":
                return new Token(TokenType.WHILE_KW, s, startPos, it.currentPos());
            case "if":
                return new Token(TokenType.IF_KW, s, startPos, it.currentPos());
            case "else":
                return new Token(TokenType.ELSE_KW, s, startPos, it.currentPos());
            case "return":
                return new Token(TokenType.RETURN_KW, s, startPos, it.currentPos());
            case "int":
                return new Token(TokenType.INT_KW, s, startPos, it.currentPos());
            case "void":
                return new Token(TokenType.VOID_KW, s, startPos, it.currentPos());
            case "double":
                return new Token(TokenType.DOUBLE_KW, s, startPos, it.currentPos());
            default:
                return new Token(TokenType.IDENT, s, startPos, it.currentPos());
        }
    }

    private Token lexOperatorOrUnknown() throws TokenizeError {
        char a = it.nextChar();
        switch (a) {
            case '+':
                return new Token(TokenType.PLUS, a, it.previousPos(), it.currentPos());
            case '-':
                if(it.peekChar() == '>'){
                    it.nextChar();
                    return new Token(TokenType.ARROW, "->", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.MINUS, a, it.previousPos(), it.currentPos());
            case '*':
                return new Token(TokenType.MUL, a, it.previousPos(), it.currentPos());
            case '/':
                if(it.peekChar() == '/'){
                    it.nextChar();
                    return new Token(TokenType.COMMENT, "//", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.DIV, a, it.previousPos(), it.currentPos());
            case '=':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.EQ, "==", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.ASSIGN, '=', it.previousPos(), it.currentPos());
            case '!':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.NEQ, "!=", it.previousPos(), it.currentPos());
                }
            case '<':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.LE, "<=", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.LT, a, it.previousPos(), it.currentPos());
            case '>':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.GE, ">=", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.GT, a, it.previousPos(), it.currentPos());
            case '(':
                return new Token(TokenType.L_PAREN, a, it.previousPos(), it.currentPos());
            case ')':
                return new Token(TokenType.R_PAREN, a, it.previousPos(), it.currentPos());
            case '{':
                return new Token(TokenType.L_BRACE, a, it.previousPos(), it.currentPos());
            case '}':
                return new Token(TokenType.R_BRACE, a, it.previousPos(), it.currentPos());
            case ',':
                return new Token(TokenType.COMMA, a, it.previousPos(), it.currentPos());
            case ':':
                return new Token(TokenType.COLON, a, it.previousPos(), it.currentPos());
            case ';':
                return new Token(TokenType.SEMICOLON, a, it.previousPos(), it.currentPos());
            case 0:
                return new Token(TokenType.EOF, "", it.previousPos(), it.currentPos());
            default:
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
