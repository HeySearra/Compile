import java.util.*;
public final class Analyser {

    Tokenizer tokenizer;
    List<Instruction> global_instructions;
    int global_slot;

    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    Definition def_table;

    // 表达式栈
    ExprStack expr_stack;


    // 暂存函数的相关内容
    Function function;
    List<Instruction> function_body;
    List<SymbolEntry> param_table;
    int param_slot;
    List<SymbolEntry> local_table;
    int local_slot;
    TokenType return_type;
    private boolean onAssign;
    private int while_level;
    //continue和break指令的集合
    List<BreakAndContinue> continue_instruction = new ArrayList<BreakAndContinue>();
    List<BreakAndContinue> break_instruction = new ArrayList<>();

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.global_instructions = new ArrayList<>();
    }

    public void analyse() throws CompileError {
        analyseProgram();
    }

    /**
     * 查看下一个 Token
     *
     * @throws TokenizeError
     * @return
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     *
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    private boolean check(TokenType... tt) throws TokenizeError {
        TokenType token = peek().getTokenType();
        while(token == TokenType.COMMENT){
            next();
            token = peek().getTokenType();
        }
        for(TokenType t: tt){
            if(token == t){
                return true;
            }
        }
        return false;
    }

    private Token expect(TokenType... tt) throws CompileError {
        TokenType token = peek().getTokenType();
        while(token == TokenType.COMMENT){
            next();
            token = peek().getTokenType();
        }
        for (TokenType t : tt) {
            if (token == t) {
                return next();
            }
        }
        throw new ExpectedTokenError(Format.generateList(tt), peek());
    }

    /**
     * 设置符号为已赋值
     *
     * @param name   符号名称
     * @param curPos 当前位置（报错用）
     * @throws AnalyzeError 如果未定义则抛异常
     */
    private void initializeSymbol(String name, Pos curPos) throws AnalyzeError {
        SymbolEntry sym = this.def_table.getSymbol(name);
        if (sym == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else if(sym.isConstant()) {
            throw new AnalyzeError(ErrorCode.AssignToConstant, curPos);
        }else if(sym.isFunction()) {
            throw new AnalyzeError(ErrorCode.AssignToFunction, curPos);
        }else{
            sym.setInitialized(true);
        }
    }

    private Instruction getLocalOrParamAddress(Token token) throws AnalyzeError {
        SymbolEntry sym = this.def_table.getSymbol(token.getValueString());
        if(sym == null)
            throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
        if(sym.getType() == SymbolType.Function)
            throw new AnalyzeError(ErrorCode.FunctionHasNoAddr, token.getStartPos());
        else if(sym.getType() == SymbolType.Param){
            return new Instruction(Operation.arga, ((long)sym.getId() + this.function.getReturnSlot()));
        }else if(sym.getType() == SymbolType.Local) {
            return new Instruction(Operation.loca, (long)sym.getId());
        }else{
            return new Instruction(Operation.globa, (long)sym.getId());
        }
    }

    private void addInstruction(Instruction ins){
        if(this.def_table.getLevel() != 0){
            this.function_body.add(ins);
        }
        else{
            this.global_instructions.add(ins);
        }
    }

    private void addAllInstruction(List<Instruction> ins){
        if(this.def_table.getLevel() != 0){
            this.function_body.addAll(ins);
        }
        else{
            this.global_instructions.addAll(ins);
        }
    }

    private void functionAddParam(TokenType tt, String name, Pos pos, boolean is_const) throws AnalyzeError {
        SymbolEntry se = this.def_table.addSymbol(this.param_slot++, name, SymbolType.Param, tt, true, is_const, pos, null, 1);
        this.param_table.add(se);
    }

    private SymbolEntry functionAddLocal(TokenType tt, String name,Boolean is_init, Boolean is_const, Pos pos, int level) throws AnalyzeError {
        SymbolEntry se = this.def_table.addSymbol(this.local_slot++, name, SymbolType.Local, tt, is_init, is_const, pos, null, level);
        this.local_table.add(se);
        return se;
    }

    public Function getStartFunction() throws AnalyzeError {
        return this.def_table.generate(this.global_instructions);
    }

    private void analyseProgram() throws CompileError {
        this.def_table = new Definition();
        this.expr_stack = new ExprStack();
        this.global_instructions = new ArrayList<>();
        while(!check(TokenType.EOF)){
            if(check(TokenType.FN_KW)){
                analyseFunction();
            }else if(check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
                analyseDeclStmt(0);
            }else{
                throw new ExpectedTokenError(
                    Format.generateList(TokenType.LET_KW, TokenType.CONST_KW, TokenType.FN_KW), peek());
            }
        }
        this.def_table.instruction = this.global_instructions;
        expect(TokenType.EOF);
    }

    private void analyseFunction() throws CompileError{
        expect(TokenType.FN_KW);
        Token nameToken = expect(TokenType.IDENT);
        this.function = null;
        this.function_body = new ArrayList<>();
        this.param_table = new ArrayList<>();
        this.param_slot = 0;
        this.local_table = new ArrayList<>();
        this.local_slot = 0;
        this.return_type = null;
        this.def_table.level = 1;
        this.onAssign = false;

        Function func = this.def_table.addFunction(nameToken.getValueString(), null, nameToken.getStartPos());
        this.function = func;
        expect(TokenType.L_PAREN);
        if(!check(TokenType.R_PAREN))
            analyseFunctionParamList();
        func.setParams(this.param_table);
        func.setParamSlot(this.param_slot);
        expect(TokenType.R_PAREN);
        expect(TokenType.ARROW);
        Token return_tt = expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW);
        this.return_type = return_tt.getTokenType();
        func.setReturnType(this.return_type);
        analyseBlockStmt(return_tt.getTokenType(), 1);
        if(this.function_body.size() == 0 || this.function_body.get(this.function_body.size() - 1).getOpt() != Operation.ret){
            this.function_body.add(new Instruction(Operation.ret));
        }
        func.setFunctionBody(this.function_body);
        func.setLocals(this.local_table);
        func.setLocalSlot(this.local_slot);
    }

    // return_type
    private void analyseBlockStmt(TokenType return_type, int level) throws CompileError{
        this.def_table.level = level;
        expect(TokenType.L_BRACE);
        while(!check(TokenType.R_BRACE)){
            analyseStmt(return_type, level);
        }
        expect(TokenType.R_BRACE);
        // 只能清符号表，不能清函数local表
        this.def_table.levelDown();
    }

    private void analyseStmt(TokenType return_type, int level) throws CompileError{
        if(check(TokenType.LET_KW) || check((TokenType.CONST_KW))){
            analyseDeclStmt(level);
        } else if(check(TokenType.IF_KW)){
            analyseIfStmt(level);
        } else if(check(TokenType.WHILE_KW)){
            analyseWhileStmt(level);
        } else if(check(TokenType.BREAK_KW)){
            analyseBreakStmt();
        } else if(check(TokenType.CONTINUE_KW)){
            analyseContinueStmt();
        } else if(check(TokenType.RETURN_KW)){
            analyseReturnStmt();
        } else if(check(TokenType.L_BRACE)){
            analyseBlockStmt(return_type, level + 1);
        } else if(check(TokenType.SEMICOLON)){
            expect(TokenType.SEMICOLON);
        } else{
            analyseExprStmt();
        }
    }

    private void analyseExprStmt() throws CompileError{
        TokenType type = analyseExpr();
        this.addAllInstruction(expr_stack.addAllReset(type));
        expect(TokenType.SEMICOLON);
    }

    private TokenType analyseExpr() throws CompileError{
        TokenType type;
        if(check(TokenType.MINUS)){
            type = analyseNegateExpr();
        }
        else if(check(TokenType.L_PAREN)){
            type = analyseGroupExpr();
        }
        else if(check(TokenType.IDENT)){
            Token nameToken = expect(TokenType.IDENT);

            if(check(TokenType.L_PAREN)) {
                type = analyseCallExpr(nameToken);
            }
            else if(check(TokenType.ASSIGN)) {
                SymbolEntry se = this.def_table.getSymbol(nameToken.getValueString());
                if(se == null){
                    throw new AnalyzeError(ErrorCode.NotDeclared, peek().getStartPos());
                }
                type = analyseAssignExpr(se, nameToken);
            }
            else{
                type = analyseIdentExpr(nameToken);
            }
        }
        else if(check(TokenType.UINT_LITERAL, TokenType.DOUBLE_LITERAL, TokenType.STRING_LITERAL, TokenType.CHAR_LITERAL)){
            type = analyseLiteralExpr();
        }
        else {
            throw new ExpectedTokenError(Format.generateList(TokenType.MINUS, TokenType.L_PAREN, TokenType.IDENT, TokenType.L_PAREN, TokenType.ASSIGN),
                next());
        }

        while(check(TokenType.PLUS, TokenType.MINUS, TokenType.MUL, TokenType.DIV, TokenType.EQ,
            TokenType.NEQ, TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE, TokenType.AS_KW)){
            if(check(TokenType.PLUS, TokenType.MINUS, TokenType.MUL, TokenType.DIV, TokenType.EQ,
                TokenType.NEQ, TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE)) {
                type = analyseOperatorExpr(type);
            }
            else if(check(TokenType.AS_KW)){
                type = analyseAsExpr(type);
            }
        }
        return type;
    }

    private TokenType analyseLiteralExpr() throws CompileError{
        Token token = expect(TokenType.UINT_LITERAL, TokenType.DOUBLE_LITERAL, TokenType.STRING_LITERAL, TokenType.CHAR_LITERAL);
        TokenType tt = token.getTokenType();
        if(tt == TokenType.UINT_LITERAL || tt == TokenType.CHAR_LITERAL){
            // 直接push进栈
            int num;
            num = (int) token.getValue();
            this.addInstruction(new Instruction(Operation.push, (long)num));
            return TokenType.INT_KW;
        }
        else if(tt == TokenType.STRING_LITERAL){
            // 新建全局变量， 变量名是该字符串，变量值也是该字符串
            int global_index = this.def_table.addGlobal(token.getValueString(),
                TokenType.STRING_LITERAL, true, true, token.getStartPos(), token.getValueString());
            this.addInstruction(new Instruction(Operation.push, (long)global_index));
            return TokenType.STRING_LITERAL;
        }
        else if(tt == TokenType.DOUBLE_LITERAL){
            String binary = Long.toBinaryString(Double.doubleToRawLongBits((Double) token.getValue()));
            this.addInstruction(new Instruction(Operation.push, Format.StringToLong(binary)));
            return TokenType.DOUBLE_KW;
        }
        else{
            throw new AnalyzeError(ErrorCode.ExpectedToken, token.getStartPos());
        }
    }

    // todo: 类型转换没写完
    private TokenType analyseAsExpr(TokenType tt) throws CompileError{
        expect(TokenType.AS_KW);
        TokenType as_tt = expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW).getTokenType();
        if(tt == TokenType.INT_KW && as_tt == TokenType.DOUBLE_KW){
            // int to double
            this.addInstruction(new Instruction(Operation.itof));
        }
        else if(tt == TokenType.DOUBLE_KW && as_tt == TokenType.INT_KW){
            // double to int
            this.addInstruction(new Instruction(Operation.ftoi));
        }
        else if(tt != as_tt){
            throw new AnalyzeError(ErrorCode.AsTypeWrong, peek().getStartPos());
        }
        while(check(TokenType.AS_KW)){
            as_tt = analyseAsExpr(as_tt);
        }
        return as_tt;
    }

    private TokenType analyseOperatorExpr(TokenType tt) throws CompileError{
        Token token = expect(TokenType.PLUS, TokenType.MINUS, TokenType.MUL, TokenType.DIV, TokenType.EQ,
            TokenType.NEQ, TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE);
        this.addAllInstruction(expr_stack.addTokenAndGenerateInstruction(token.getTokenType(), tt));
        TokenType next_tt = analyseExpr();
        if(tt != next_tt || (tt != TokenType.INT_KW && tt != TokenType.DOUBLE_KW)){
            throw new AnalyzeError(ErrorCode.ExprTypeWrong, peek().getStartPos());
        }
        return next_tt;
    }

    private TokenType analyseIdentExpr(Token token) throws CompileError{
        SymbolEntry se = this.def_table.getSymbol(token.getValueString());
        if(se == null){
            throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
        }
        if(!this.def_table.getSymbol(token.getValueString()).isInitialized()){
            throw new AnalyzeError(ErrorCode.NotInitialized, token.getStartPos());
        }
        this.addInstruction(getLocalOrParamAddress(token));
        this.addInstruction(new Instruction(Operation.load64));
        return se.getTokenType();
    }

    private TokenType analyseAssignExpr(SymbolEntry se, Token token) throws CompileError{
        expect(TokenType.ASSIGN);
        if(this.onAssign){
            throw new AnalyzeError(ErrorCode.AssignFaild, peek().getStartPos());
        }
        if (se.isConstant())
            throw new AnalyzeError(ErrorCode.AssignToConstant, peek().getStartPos());

        this.addInstruction(getLocalOrParamAddress(token));
        // 获取值的类型
        TokenType type = analyseExpr();
        TokenType assigned = se.getTokenType();
        // 判断值的类型是否和se相同
        if(type != assigned || (assigned != TokenType.INT_KW && assigned != TokenType.DOUBLE_KW)){
            throw new AnalyzeError(ErrorCode.AssignTypeWrong, peek().getStartPos());
        }
        this.addAllInstruction(expr_stack.addAllReset(type));
        System.out.println("ini:: " + token.getValueString());
        initializeSymbol(token.getValueString(), token.getStartPos());
        this.addInstruction(new Instruction(Operation.store64));
        return TokenType.VOID_KW;
    }

    private TokenType analyseCallExpr(Token token) throws CompileError{
        // 返回函数的返回值
        Function func = this.def_table.getFunction(token.getValueString());

        // 分配return的slot
        this.addInstruction(new Instruction(Operation.stackalloc, (long)func.getReturnSlot()));
        expect(TokenType.L_PAREN);
        this.expr_stack.operation_stack.push(TokenType.L_PAREN);
        if(!check(TokenType.R_PAREN)) {
            // 准备参数，分配空间并放入参数
            analyseCallParamList(func.getParams());
        }
        expect(TokenType.R_PAREN);
        System.out.println("top  " + this.expr_stack.operation_stack.pop());

        if(func.isSTDFunction()){
            this.addInstruction(new Instruction(Operation.callname, (long)func.getId()));
        }
        else{

            this.addInstruction(new Instruction(Operation.call, (long)this.def_table.getFunctionIndex(func)));
        }
        return func.getReturnType();
    }

    private void analyseCallParamList(List<SymbolEntry> param_list) throws CompileError{
        int param_num = 1, i = 0;
        TokenType type = analyseExpr();
        if(param_list.get(i++).getTokenType() != type){
            throw new AnalyzeError(ErrorCode.ExprTypeWrong, peek().getStartPos());
        }
        while (!this.expr_stack.operation_stack.empty() && this.expr_stack.operation_stack.peek() != TokenType.L_PAREN) {
            this.addAllInstruction(this.expr_stack.generateInstruction(this.expr_stack.operation_stack.pop(), type));
        }
        while(check(TokenType.COMMA)){
            expect(TokenType.COMMA);
            type = analyseExpr();
            if(param_list.get(i++).getTokenType() != type){
                throw new AnalyzeError(ErrorCode.ExprTypeWrong, peek().getStartPos());
            }
            while (!this.expr_stack.operation_stack.empty() && this.expr_stack.operation_stack.peek() != TokenType.L_PAREN) {
                this.addAllInstruction(this.expr_stack.generateInstruction(this.expr_stack.operation_stack.pop(), type));
            }
            param_num++;
        }
        if(param_num != param_list.size()){
            System.out.println("当前参数个数：" + param_num + " ，期望参数个数：" + param_list.size());
            throw new AnalyzeError(ErrorCode.ParamNumWrong, this.peekedToken.getStartPos());
        }
    }

    private TokenType analyseGroupExpr() throws CompileError{
        expect(TokenType.L_PAREN);
        this.expr_stack.push(TokenType.L_PAREN);
        TokenType type = analyseExpr();
        expect(TokenType.R_PAREN);
        this.addAllInstruction(this.expr_stack.addTokenAndGenerateInstruction(TokenType.R_PAREN, type));
        this.addAllInstruction(expr_stack.addAllReset(type));
        return type;
    }

    private TokenType analyseNegateExpr() throws CompileError{
        expect(TokenType.MINUS);
        this.addInstruction(new Instruction(Operation.push, 0L));
        TokenType tt = analyseExpr();
        if(tt == TokenType.INT_KW)
            this.addInstruction(new Instruction((Operation.sub_i)));
        else
            this.addInstruction(new Instruction((Operation.sub_f)));
        return tt;
    }

    private void analyseReturnStmt() throws CompileError{
        expect(TokenType.RETURN_KW);
        if(!check(TokenType.SEMICOLON)){
            // 有返回值
            if(this.return_type == TokenType.VOID_KW){
                throw new AnalyzeError(ErrorCode.ReturnTypeWrong, peekedToken.getStartPos());
            }
            // todo: 返回值类型检查
            // 返回值off是0
            this.addInstruction(new Instruction(Operation.arga, (long)0));
            TokenType type = analyseExpr();
            if(type != this.return_type){
                throw new AnalyzeError(ErrorCode.ReturnTypeWrong, peek().getStartPos());
            }
            this.addAllInstruction(expr_stack.addAllReset(type));
            this.addInstruction(new Instruction(Operation.store64));
        }
        else if(this.return_type != TokenType.VOID_KW){
            throw new AnalyzeError(ErrorCode.ReturnTypeWrong, peek().getStartPos());
        }
        this.addInstruction(new Instruction(Operation.ret));
        expect(TokenType.SEMICOLON);
    }

    private void analyseContinueStmt() throws CompileError{
        expect(TokenType.CONTINUE_KW);
        expect(TokenType.SEMICOLON);
        if(this.while_level == 0){
            throw new AnalyzeError(ErrorCode.OutWhile, peek().getStartPos());
        }
        Instruction instruction = new Instruction(Operation.br);
        this.continue_instruction.add(new BreakAndContinue(instruction, this.function_body.size() + 1, this.while_level));
        this.function_body.add(instruction);
    }

    private void analyseBreakStmt() throws CompileError{
        expect(TokenType.BREAK_KW);
        if(this.while_level == 0){
            throw new AnalyzeError(ErrorCode.OutWhile, peek().getStartPos());
        }
        Instruction instruction = new Instruction(Operation.br);
        this.break_instruction.add(new BreakAndContinue(instruction, this.function_body.size() + 1, this.while_level));
        this.function_body.add(instruction);

        expect(TokenType.SEMICOLON);
    }

    private void analyseWhileStmt(int level) throws CompileError{
        // 函数里的第一个while level为1
        expect(TokenType.WHILE_KW);
        this.addInstruction(new Instruction(Operation.br, (long)0));

        // start，记录开始计算while条件的指令位置
        int start = this.function_body.size();
        TokenType type = analyseExpr();
        this.addAllInstruction(expr_stack.addAllReset(type));

        if(type != TokenType.INT_KW && type != TokenType.DOUBLE_KW){
            throw new AnalyzeError(ErrorCode.ExprTypeWrong, peek().getStartPos());
        }

        // br_true，如果是真的话跳过br指令，如果是假的话跳到br指令跳出循环
        this.addInstruction(new Instruction(Operation.br_true, (long)1));

        //br，跳出循环体，参数待填
        Instruction br = new Instruction(Operation.br);
        this.addInstruction(br);

        // 记录while循环体开始处
        int index = this.function_body.size();

        this.while_level++;
        analyseBlockStmt(null, level + 1);
        if(this.while_level > 0){
            this.while_level--;
        }

        // br_start，跳到while条件判断处，参数待填
        Instruction br_start = new Instruction(Operation.br);
        this.addInstruction(br_start);
        br_start.setNum((long)(start - this.function_body.size()));

        int end_index = this.function_body.size();
        br.setNum((long)(end_index - index));

        if(break_instruction.size()!=0){
            for(BreakAndContinue b: break_instruction){
                if(b.getWhileLevel() == this.while_level + 1)
                    b.getInstruction().setNum((long)(end_index - b.getLocation()));
            }
        }

        if(continue_instruction.size() != 0){
            for(BreakAndContinue c: continue_instruction){
                if(c.getWhileLevel() == this.while_level + 1)
                    c.getInstruction().setNum((long)(end_index - c.getLocation() - 1));
            }
        }

        // 重新初始化
        if(this.while_level == 0){
            this.continue_instruction = new ArrayList<>();
            this.break_instruction = new ArrayList<>();
        }
    }

    private void analyseIfStmt(int level) throws CompileError{
        expect(TokenType.IF_KW);

        TokenType type = analyseExpr();
        this.addAllInstruction(expr_stack.addAllReset(type));

        //brTrue
        this.addInstruction(new Instruction(Operation.br_true, (long)1));
        //br
        Instruction br = new Instruction(Operation.br, (long)0);
        this.addInstruction(br);
        int index = this.function_body.size();

        analyseBlockStmt(null, level + 1);

        int size = this.function_body.size();

        if(this.function_body.get(size - 1).getOpt() == Operation.ret){
            // 如果if block分析完成后最后一个语句是ret
            int dis = size - index;
            br.setNum((long)dis);
            if(check(TokenType.ELSE_KW)){
                expect(TokenType.ELSE_KW);
                if(check(TokenType.IF_KW)){
                    // else if，递归调用if分析
                    analyseIfStmt(level);
                }
                else{
                    // else 语句
                    analyseBlockStmt(null, level + 1);
                    this.addInstruction(new Instruction(Operation.br, (long)0));
                }
            }
        }
        else{
            // if执行完成后要跳转到else block之后
            Instruction jumpInstruction = new Instruction(Operation.br);
            this.addInstruction(jumpInstruction);
            int jump = this.function_body.size();

            int dis = jump - index;
            br.setNum((long)dis);

            if(check(TokenType.ELSE_KW)){
                expect(TokenType.ELSE_KW);
                if(check(TokenType.IF_KW)){
                    // else if，递归调用if分析
                    analyseIfStmt(level);
                }
                else{
                    // else 语句
                    analyseBlockStmt(null, level + 1);
                    this.addInstruction(new Instruction(Operation.br, (long)0));
                }
            }
            dis = this.function_body.size() - jump;
            jumpInstruction.setNum((long)dis);
        }
    }

    private void analyseFunctionParamList() throws CompileError{
        analyseFunctionParam();
        while(check(TokenType.COMMA)){
            expect(TokenType.COMMA);
            analyseFunctionParam();
        }

    }

    private void analyseFunctionParam() throws CompileError{
        boolean is_const = false;
        if(check(TokenType.CONST_KW)){
            expect(TokenType.CONST_KW);
            is_const = true;
        }
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        Token type = expect(TokenType.VOID_KW, TokenType.INT_KW, TokenType.DOUBLE_KW);
        functionAddParam(type.getTokenType(), nameToken.getValueString(), nameToken.getStartPos(), is_const);
    }

    // level == 0是全局
    private void analyseDeclStmt(int level) throws CompileError{
        if(check(TokenType.LET_KW)){
            analyseLetDeclStmt(level);
        } else if (check(TokenType.CONST_KW)){
            analyseConstDeclStmt(level);
        }
    }

    // level == 0是全局
    private void analyseConstDeclStmt(int level) throws CompileError{
        expect(TokenType.CONST_KW);
        Token nameToken = expect(TokenType.IDENT);
        if(this.def_table.getSymbol(nameToken.getValueString()) != null){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, nameToken.getStartPos());
        }
        expect(TokenType.COLON);
        Token type = expect(TokenType.INT_KW, TokenType.DOUBLE_KW);
        expect(TokenType.ASSIGN);
        this.onAssign = true;
        if(level == 0){// 全局
            int global_id = this.def_table.addGlobal(nameToken.getValueString(), type.getTokenType(), true, true, nameToken.getStartPos(), null);
            this.global_instructions.add(new Instruction(Operation.globa, (long)global_id));
        }
        else{
            SymbolEntry se = functionAddLocal(type.getTokenType(),nameToken.getValueString(), true, true, nameToken.getStartPos(), level);
            this.addInstruction(new Instruction(Operation.loca, (long)se.getId()));
        }
        TokenType tt = analyseExpr();
        if(tt != type.getTokenType()){
            throw new AnalyzeError(ErrorCode.ExprTypeWrong, peek().getStartPos());
        }
        this.addAllInstruction(this.expr_stack.addAllReset(tt));
        this.addInstruction(new Instruction((Operation.store64)));
        this.onAssign = false;
        expect(TokenType.SEMICOLON);
    }

    private void analyseLetDeclStmt(int level) throws CompileError{
        expect(TokenType.LET_KW);
        Token nameToken = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        Token type = expect(TokenType.INT_KW, TokenType.DOUBLE_KW);
        // todo: double类型检查
        if(check(TokenType.ASSIGN)){
            expect(TokenType.ASSIGN);
            this.onAssign = true;
            if(level == 0){
                // 全局变量
                int global_id = this.def_table.addGlobal(nameToken.getValueString(), type.getTokenType(), true, false, nameToken.getStartPos(), null);
                this.global_instructions.add(new Instruction(Operation.globa, (long)global_id));
            }
            else{
                // 局部变量
                SymbolEntry se = functionAddLocal(type.getTokenType(),nameToken.getValueString(), true, false, nameToken.getStartPos(), level);
                this.addInstruction(new Instruction(Operation.loca, (long)se.getId()));
            }
            TokenType tt = analyseExpr();
            if(tt != type.getTokenType()){
                System.out.println(tt + "  " + type.getTokenType());
                throw new AnalyzeError(ErrorCode.ExprTypeWrong, peek().getStartPos());
            }
            this.addAllInstruction(this.expr_stack.addAllReset(tt));
            this.addInstruction(new Instruction((Operation.store64)));
            this.onAssign = false;
        }
        else{
            if(level == 0){
                // 全局变量
                this.def_table.addGlobal(nameToken.getValueString(), type.getTokenType(), true, false, nameToken.getStartPos(), null);
            }
            else{
                // 局部变量
                functionAddLocal(type.getTokenType(),nameToken.getValueString(), false, false, nameToken.getStartPos(), level);
            }
        }
        expect(TokenType.SEMICOLON);
    }
}
