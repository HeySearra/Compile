import java.util.ArrayList;
import java.util.List;

public class ExprTree {
  public static List<TokenType> operation_stack =new ArrayList<>();
  //  + - * / ( )
  static int[][] priority ={
      {1,1,-1,-1,-1,1},
      {1,1,-1,-1,-1,1},
      {1,1,1,1,-1,1},
      {1,1,1,1,-1,1},
      {-1,-1,-1,-1,-1,100},
      {1,1,1,1,0,0}
  };

  static int getIndex(TokenType tokenType){
    if(tokenType == TokenType.PLUS){
      return 0;
    }else if(tokenType == TokenType.MINUS){
      return 1;
    }else if(tokenType == TokenType.MUL){
      return 2;
    }else if(tokenType == TokenType.DIV){
      return 3;
    }else if(tokenType == TokenType.L_PAREN){
      return 4;
    }else if(tokenType == TokenType.R_PAREN){
      return 5;
    }
    return -1;
  }

  private static Operation convertToOperation(TokenType tt) {
    if(tt == TokenType.PLUS){
      return Operation.add_i;
    }else if(tt == TokenType.MINUS){
      return Operation.sub_i;
    }else if(tt == TokenType.MUL){
      return Operation.mul_i;
    }else{
      return Operation.div_i;
    }
  }


  public static List<Instruction> addTokenAndGenerateInstruction(TokenType tt){
    List<Instruction> instructions=new ArrayList<>();
    if (operation_stack.size() < 1){
      operation_stack.add(tt);
      return instructions;
    }

    TokenType top= operation_stack.get(operation_stack.size() - 1);
    System.out.println("top:" + top);
    while (priority[getIndex(top)][getIndex(tt)] > 0){
      operation_stack.remove(operation_stack.size()-1);
      if(top == TokenType.PLUS){
        instructions.add(new Instruction(Operation.add_i));
      } else if(top == TokenType.MINUS){
        instructions.add(new Instruction(Operation.sub_i));
      } else if(top == TokenType.MUL){
        instructions.add(new Instruction(Operation.mul_i));
      } else if(top == TokenType.DIV){
        instructions.add(new Instruction(Operation.div_i));
      } else if(top == TokenType.L_PAREN){
        return instructions;
      }
      if (operation_stack.size()==0)
        break;
      top = operation_stack.get(operation_stack.size()-1);
    }
    operation_stack.add(tt);
    return instructions;
  }

  public static List<Instruction> addAllReset(){
    List<Instruction> instructions=new ArrayList<>();
    for(int i = operation_stack.size()-1;i>=0;i--){
      instructions.add(new Instruction(convertToOperation(operation_stack.get(i))));
      operation_stack.remove(i);
    }
    return instructions;
  }

}
