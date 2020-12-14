public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了
    /**
     * 获取下一个 Token
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
            return lexUIntOrDouble();
        } else if (Character.isAlphabetic(peek)) {
            return lexIdentOrKeyword();
        } else if (peek == '\"'){
            return lexStringLiteral();
        } else if (peek == '\''){
            return lexCharLiteral();
        }
        else {
            return lexOperatorOrCommentOrUnknown();
        }
    }

    private void lexComment() throws TokenizeError{
        if (it.isEOF()){
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (!Character.isDigit(it.peekChar())) {
            throw new TokenizeError(ErrorCode.InvalidInput, it.currentPos());
        }
        char peek = it.peekChar();
        if(peek != '/'){
            throw new TokenizeError(ErrorCode.InvalidInput, it.currentPos());
        }
        while(!it.isEOF() && peek != '\n'){
            peek = it.nextChar();
        }
        if(!it.isEOF()){
            it.nextChar();
        }
    }

    private Token lexUIntOrDouble() throws TokenizeError {
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
        StringBuilder uint=new StringBuilder("");
        char peek = it.peekChar();
        while(!it.isEOF() && Character.isDigit(peek)){
            uint.append(it.nextChar());
            peek = it.peekChar();
        }
        Token token;
        if (peek == '.'){
            it.nextChar();
            uint.append('.');
            peek = it.peekChar();
            while(!it.isEOF() && Character.isDigit(peek)){
                uint.append(it.nextChar());
                peek = it.peekChar();
            }
            if(peek == 'e' || peek == 'E'){
                uint.append(it.nextChar());
                peek = it.peekChar();
                if(peek == '+' || peek == '-'){
                    uint.append(it.nextChar());
                    peek = it.peekChar();
                }
                while(!it.isEOF() && Character.isDigit(peek)){
                    uint.append(it.nextChar());
                    peek = it.peekChar();
                }
            }
            double num;
            try{
                num = Double.parseDouble(uint.toString());
            }catch(Exception e) {
                throw new TokenizeError(ErrorCode.InvalidDouble, it.currentPos());
            }
            token = new Token(TokenType.DOUBLE_LITERAL, num, startPos, it.currentPos());
        }
        else{
            int num;
            try{
                num = Integer.parseInt(uint.toString());
                token = new Token(TokenType.UINT_LITERAL, num, startPos, it.currentPos());
            }catch(Exception e){
                try{
                    double double_num = Double.parseDouble(uint.toString());
                    token = new Token(TokenType.DOUBLE_LITERAL, double_num, startPos, it.currentPos());
                }catch(Exception e1) {
                    throw new TokenizeError(ErrorCode.InvalidDouble, it.currentPos());
                }
            }
        }
        return token;
    }

    private Token lexCharLiteral() throws TokenizeError {
        Pos startPos;
        try {
            startPos = it.currentPos();
        } catch (Error e) {
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (it.isEOF()) {
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (it.peekChar() != '\'') {
            throw new TokenizeError(ErrorCode.InvalidInput, it.currentPos());
        }
        it.nextChar();
        char res;
        char peek = it.peekChar();
        if(Format.isCharRegularChar(peek)){
            res = it.nextChar();
        }
        else if(peek == '\\') {
            it.nextChar();
            peek = it.peekChar();
            switch (peek) {
                case '\'':
                    res = '\'';
                    break;
                case '\"':
                    res = '\"';
                    break;
                case '\\':
                    res = '\\';
                    break;
                case 'n':
                    res = '\n';
                    break;
                case 'r':
                    res = '\r';
                    break;
                case 't':
                    res = '\t';
                    break;
                default:
                    throw new TokenizeError(ErrorCode.InvalidInput, it.currentPos());
            }
            it.nextChar();
        }
        else{
            throw new TokenizeError(ErrorCode.InvalidChar, it.currentPos());
        }
        if(it.peekChar() != '\''){
            throw new TokenizeError(ErrorCode.InvalidChar, it.currentPos());
        }
        else{
            it.nextChar();
            return new Token(TokenType.CHAR_LITERAL, (int)  res, startPos, it.currentPos());
        }
    }

    private Token lexStringLiteral() throws TokenizeError {
        Pos startPos;
        try{
            startPos = it.currentPos();
        }catch(Error e){
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (it.isEOF()){
            throw new TokenizeError(ErrorCode.EOF, it.currentPos());
        }
        if (it.peekChar() != '\"') {
            throw new TokenizeError(ErrorCode.InvalidInput, it.currentPos());
        }
        it.nextChar();
        StringBuilder str=new StringBuilder("");
        char peek = it.peekChar();
        while(!it.isEOF() && peek != '\"'){
            if (peek == '\\'){
                it.nextChar();
                peek = it.peekChar();
                switch (peek){
                    case '\'':
                        str.append('\'');
                        break;
                    case '\"':
                        str.append('\"');
                        break;
                    case '\\':
                        str.append('\\');
                        break;
                    case 'n':
                        str.append('\n');
                        break;
                    case 'r':
                        str.append('\r');
                        break;
                    case 't':
                        str.append('\t');
                        break;
                    default:
                        throw new TokenizeError(ErrorCode.InvalidInput, it.currentPos());
                }
                it.nextChar();
            }
            else{
                str.append(it.nextChar());
            }
            peek = it.peekChar();
        }
        if(peek != '\"'){
            throw new TokenizeError(ErrorCode.InvalidString, it.currentPos());
        }
        else{
            it.nextChar();
            return new Token(TokenType.STRING_LITERAL, str.toString(), startPos, it.currentPos());
        }
    }

    private Token lexIdentOrKeyword() throws TokenizeError {
        StringBuilder b = new StringBuilder("");
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
        while (!it.isEOF() && (Character.isLetterOrDigit(it.peekChar()) || it.peekChar() == '_')) {
            b.append(it.nextChar());
        }
        String s = b.toString();
        switch (s) {
            case "fn": return new Token(TokenType.FN_KW, s, startPos, it.currentPos());
            case "let": return new Token(TokenType.LET_KW, s, startPos, it.currentPos());
            case "const": return new Token(TokenType.CONST_KW, s, startPos, it.currentPos());
            case "as": return new Token(TokenType.AS_KW, s, startPos, it.currentPos());
            case "while": return new Token(TokenType.WHILE_KW, s, startPos, it.currentPos());
            case "if": return new Token(TokenType.IF_KW, s, startPos, it.currentPos());
            case "else": return new Token(TokenType.ELSE_KW, s, startPos, it.currentPos());
            case "return": return new Token(TokenType.RETURN_KW, s, startPos, it.currentPos());
            case "int":
                return new Token(TokenType.INT_KW, s, startPos, it.currentPos());
            case "void":
                return new Token(TokenType.VOID_KW, s, startPos, it.currentPos());
            case "double":
                return new Token(TokenType.DOUBLE_KW, s, startPos, it.currentPos());
            case "break":
                return new Token(TokenType.BREAK_KW, s, startPos, it.currentPos());
            case "continue":
                return new Token(TokenType.CONTINUE_KW, s, startPos, it.currentPos());
            default:
                return new Token(TokenType.IDENT, s, startPos, it.currentPos());
        }
    }

    private Token lexOperatorOrCommentOrUnknown() throws TokenizeError {
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
                    StringBuilder comment=new StringBuilder("");
                    char peek = it.peekChar();
                    while(!it.isEOF() && peek != '\n'){
                        comment.append(it.nextChar());
                        peek = it.peekChar();
                    }
                    return new Token(TokenType.COMMENT, "//" + comment.toString(), it.previousPos(), it.currentPos());
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
