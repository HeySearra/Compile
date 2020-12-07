package analyser;


import instruction.Instruction;
import java.util.ArrayList;
import java.util.List;
import tokenizer.Token;
import tokenizer.TokenType;
import util.Pos;

public class Function {
  // 函数体
  private List<Instruction> function_body;
  // 函数编号，开始函数编号为0
  private int id;
  // 参数
  private List<Token> params;
  // 局部变量
  private List<Token> lets;
  // 函数名
  private String name;
  // 返回值类型
  private TokenType return_type;
  // 定义位置
  private Pos pos;
  // 符号表
  private List<SymbolEntry> symbol_table;

  public Function(String name, Pos pos, TokenType return_type) {
    this.name = name;//main 这种
    this.pos = pos;
    this.function_body = new ArrayList<>();
    this.params = new ArrayList<>();

    this.symbol_table = new ArrayList<>();
    this.return_type = return_type;
  }

  public void setFunctionBody(List<Instruction> function_body) { this.function_body = function_body; }

  public List<Token> getParams() { return params; }

  public void setParams(List<Token> params) { this.params = params; }

  public int getId() { return id; }

  public void setId(int id) { this.id = id; }

  public List<Instruction>  getFunctionBody(){ return this.function_body; }

  public List<SymbolEntry> getSymbolTable() { return this.symbol_table; }

  public TokenType getReturnType() { return this.return_type; }

  public String getName() { return name; }

  public Pos getPos() { return pos; }

  public int getParamNum() { return this.params.size(); }

//  public int getVarNmum() { return this.let_table.size(); }

  public int getReturnNum() {
    if (this.return_type!= TokenType.VOID_KW)
      return 1;
    else return 0;
  }

  public void setPos(Pos pos) {
    this.pos = pos;
  }

  public void setReturnType(TokenType return_type) {
    this.return_type = return_type;
  }

//  public void addSymbolEntry(SymbolEntry symbolEntry) {
//    this.symbol_table.add(symbolEntry);
//    if(symbolEntry.getType() == SymbolType.Param)
//      this.paramSoltNum++;
//    else{
//      int currentvarSoltNUm = this.symbolEntries.size() - this.paramSoltNum;
//      if(currentvarSoltNUm > this.varSoltNmum)
//        this.varSoltNmum = currentvarSoltNUm;
//    }
//  }
//
//  public void outDeep(int deep) {
//    int i = symbolEntries.size()-1;
//    for(;i>=0;i--){
//      if (symbolEntries.get(i).getNametype() == SymbolType.Param)
//        return;
//      if(symbolEntries.get(i).getDeep()==deep)
//        symbolEntries.remove(i);
//      else break;
//    }
//  }
}
