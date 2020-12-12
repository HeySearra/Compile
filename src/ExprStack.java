import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ExprStack {
  public static Stack<TokenType> operation_stack =new Stack<>();
  //  + - * / ( )
  int[][] priority ={
      {1,1,-1,-1,-1,1,   1,1,1,1,1,1,  -1},
      {1,1,-1,-1,-1,1,  1,1,1,1,1,1,   -1},
      {1,1,1,1,-1,1,   1,1,1,1,1,1,    -1},
      {1,1,1,1,-1,1,   1,1,1,1,1,1,   -1},

      {-1,-1,-1,-1,-1,100,   -1,-1,-1,-1,-1,-1,   -1},
      {-1,-1,-1,-1,0,0   ,    -1,-1,-1,-1,-1,-1   ,-1},

      {-1,-1,-1,-1,-1,1,  1,1,1,1,1,1,      -1},
      {-1,-1,-1,-1,-1,1,  1,1,1,1,1,1,      -1},
      {-1,-1,-1,-1,-1,1,  1,1,1,1,1,1,      -1},
      {-1,-1,-1,-1,-1,1,  1,1,1,1,1,1,      -1},
      {-1,-1,-1,-1,-1,1,  1,1,1,1,1,1,      -1},
      {-1,-1,-1,-1,-1,1,  1,1,1,1,1,1,      -1},

      {1,1,1,1,-1,1,     1,1,1,1,1,1    ,-1}

  };

  // 当前计算结果的类型
  TokenType type;

  public int getIndex(TokenType tokenType){
    if(tokenType == TokenType.PLUS){
      return 0;
    } else if(tokenType == TokenType.MINUS){
      return 1;
    } else if(tokenType == TokenType.MUL){
      return 2;
    } else if(tokenType == TokenType.DIV){
      return 3;
    } else if(tokenType == TokenType.L_PAREN){
      return 4;
    } else if(tokenType == TokenType.R_PAREN){
      return 5;
    } else if(tokenType== TokenType.LT){
      return 6;
    } else if(tokenType== TokenType.GT){
      return 7;
    } else if(tokenType== TokenType.LE){
      return 8;
    } else if(tokenType== TokenType.GE){
      return 9;
    } else if(tokenType== TokenType.EQ){
      return 10;
    } else if(tokenType== TokenType.NEQ){
      return 11;
    }
    return -1;
  }

  public List<Instruction> generateInstruction(TokenType top){
    List<Instruction> res_ins = new ArrayList<>();
    switch (top) {
      case LT:
        res_ins.add(new Instruction(Operation.cmp_i));
        res_ins.add(new Instruction(Operation.set_lt));
        break;
      case LE:
        res_ins.add(new Instruction(Operation.cmp_i));
        res_ins.add(new Instruction(Operation.set_gt));
        res_ins.add(new Instruction(Operation.not));
        break;
      case GT:
        res_ins.add(new Instruction(Operation.cmp_i));
        res_ins.add(new Instruction(Operation.set_gt));
        break;
      case GE:
        res_ins.add(new Instruction(Operation.cmp_i));
        res_ins.add(new Instruction(Operation.set_lt));
        res_ins.add(new Instruction(Operation.not));
        break;
      case PLUS:
        res_ins.add(new Instruction(Operation.add_i));
        break;
      case MINUS:
        res_ins.add(new Instruction(Operation.sub_i));
        break;
      case MUL:
        res_ins.add(new Instruction(Operation.mul_i));
        break;
      case DIV:
        res_ins.add(new Instruction(Operation.div_i));
        break;
      case EQ:
        res_ins.add(new Instruction(Operation.cmp_i));
        res_ins.add(new Instruction(Operation.not));
        break;
      case NEQ:
        res_ins.add(new Instruction(Operation.cmp_i));
        break;
    }
    return res_ins;
  }


  public List<Instruction> addTokenAndGenerateInstruction(TokenType tt){
    List<Instruction> instructions=new ArrayList<>();
    if (operation_stack.size() < 1){
      operation_stack.add(tt);
      return instructions;
    }

    TokenType top= operation_stack.peek();
    System.out.println("top:" + top);
    while (priority[getIndex(top)][getIndex(tt)] > 0){
      operation_stack.pop();
      instructions.addAll(generateInstruction(top));
      if (operation_stack.empty() || top == TokenType.L_PAREN)
        break;
      top = operation_stack.peek();
    }
    operation_stack.push(tt);
    return instructions;
  }

  public List<Instruction> addAllReset(){
    List<Instruction> instructions=new ArrayList<>();
    while(!operation_stack.empty()){
      instructions.addAll(generateInstruction(operation_stack.pop()));
    }
    return instructions;
  }
}
