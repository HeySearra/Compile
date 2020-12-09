import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Definition {
    List<SymbolEntry> global_list = new ArrayList<SymbolEntry>();

    HashMap<String, Function> function_list = new HashMap<String, Function>();

    // 全局变量加函数的符号表
    HashMap<Integer, SymbolEntry> symbol_list = new HashMap<>();

    int nextOffset = 0;

    int symbol_list_index = 1;

    List<Instruction> instruction;

    // 当前level
    int level = 0;

    public Definition() throws AnalyzeError {
        String[] std_function = {"getint", "getdouble", "getchar", "putint", "putdouble", "putchar",
            "putstr", "putln"};
        this.addFunction("getint", TokenType.INT_KW, new Pos(-1, -1), 0, new ArrayList<Instruction>(Collections.singletonList(new Instruction())));
        this.addFunction("getdouble", TokenType.DOUBLE_KW, new Pos(-1, -1), 0, new ArrayList<Instruction>(Collections.singletonList(new Instruction())));
        this.addFunction("getchar", TokenType.INT_KW, new Pos(-1, -1), 0, new ArrayList<Instruction>(Collections.singletonList(new Instruction())));
        this.addFunction("putint", TokenType.VOID_KW, new Pos(-1, -1), 0, new ArrayList<Instruction>(Collections.singletonList(new Instruction())));
        this.addFunction("putdouble", TokenType.VOID_KW, new Pos(-1, -1), 0, new ArrayList<Instruction>(Collections.singletonList(new Instruction())));
        this.addFunction("putchar", TokenType.VOID_KW, new Pos(-1, -1), 0, new ArrayList<Instruction>(Collections.singletonList(new Instruction())));
        this.addFunction("putstr", TokenType.VOID_KW, new Pos(-1, -1), 0, new ArrayList<Instruction>(Collections.singletonList(new Instruction())));
        this.addFunction("putln", TokenType.DOUBLE_KW, new Pos(-1, -1), 0, new ArrayList<Instruction>(Collections.singletonList(new Instruction())));
    }

    public int addGlobal(SymbolType type, String name, TokenType tt, boolean is_ini, boolean is_const, Pos pos, Object value) throws AnalyzeError {
        if(getSymbol(name) != null){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, pos);
        }
        SymbolEntry se = new SymbolEntry(type, name, tt, is_ini, is_const, this.global_list.size(), value, 0);
        this.global_list.add(se);
        int index = this.symbol_list.size();
        this.symbol_list.put(this.symbol_list.size(), se);
        return index;
    }

    public int getGlobalId(String name) throws AnalyzeError {
        for(SymbolEntry se: global_list){
            if(se.getName().equals(name)){
                return global_list.indexOf(se);
            }
        }
        throw new AnalyzeError(ErrorCode.NoSuchGlobal, new Pos(-1, -1));
    }

    public List<SymbolEntry> getGlobalList() { return this.global_list; }

    public int getGlobalListCount(){ return this.global_list.size(); }

    public HashMap<String, Function> getFunctionList() { return function_list; }

    public int getFunctionListCount(){ return this.function_list.size(); }

    public Function addFunction(String name, TokenType return_tt, Pos pos, int stack_off, List<Instruction> ins) throws AnalyzeError {
        if(this.function_list.get(name) != null){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, pos);
        }
        Function func;
        if(name.equals("_start")){
            func = new Function(name, pos, return_tt, ins, 0);
            function_list.put(name, func);
        }
        else{
            func = new Function(name, pos, return_tt, ins, this.function_list.size() + 1);
            function_list.put(name, func);
        }
        this.addSymbol(name, SymbolType.Function, return_tt, true, true, pos, this.symbol_list.size() , null, 0);
        return func;
    }

    public void addSymbol(String name, SymbolType type, TokenType tt, boolean is_init, boolean is_const, Pos curPos, int off, Object value, int level) throws AnalyzeError {
        System.out.println("add symbol " + name + " index: " + this.symbol_list_index + " type: " + tt + " level: " + level);
        if(getSymbol(name) != null){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        }

        this.symbol_list.put(this.symbol_list_index++, new SymbolEntry(type, name, tt, is_init, is_const, off, value, level));
        System.out.println(this.symbol_list.size());
    }

    public SymbolEntry getSymbol(String name){
        // todo: 从level最高的开始查， hashmap排序
        System.out.println("get symbol: " + name + " level: " + this.level);
        for(int i = this.level; i >= 0; i--){
            for(Integer a :this.symbol_list.keySet()){
                SymbolEntry sym = this.symbol_list.get(a);
//                System.out.println("check name: " +sym.getName());
                if(sym.level <= i && sym.getName().equals(name)){
                    return sym;
                }
            }
        }
        return null;
    }

    public Function getFunction(String name){
        return this.function_list.get(name);
    }

    public Function generate() throws AnalyzeError {
        Function main_func = getFunction("main");
        if(main_func == null){
            throw new AnalyzeError(ErrorCode.CantFindMain, new Pos(0,0));
        }
        Function start_func = addFunction("_start",TokenType.VOID_KW, new Pos(0, 0), 0, main_func.getFunctionBody());
        List<Instruction> instructions = new ArrayList<>();
        instructions.addAll(main_func.getFunctionBody());
        instructions.add(new Instruction(Operation.stackalloc,(long)main_func.getReturnSlot()));
        instructions.add(new Instruction(Operation.call, (long)main_func.getId()));
        return start_func;
    }
    public void levelDown(){
        HashMap<Integer, SymbolEntry> res = new HashMap<>();
        for(Integer i: this.symbol_list.keySet()){
            if(this.symbol_list.get(i).getLevel() == this.level){
                res.put(i, this.symbol_list.get(i));
                System.out.println("remove symbol " + this.symbol_list.get(i).getName() + " type: " + this.symbol_list.get(i).getType() + " level: " + this.level);
            }
        }
        for(Integer i : res.keySet()){
            this.symbol_list.remove(i);
        }
        this.level--;
    }
}
