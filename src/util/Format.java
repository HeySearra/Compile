package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import tokenizer.TokenType;

public class Format {
  public static Boolean isEscapeSequence(char a){
    return a == '\\' || a == '\"' || a == '\'' || a == '\n' || a == '\r' || a == '\t';
  }

  public static Boolean isStringRegularChar(char a){
    return a != '\\' && a != '\"' && a != '\n' && a != '\r' && a != '\t';
  }

  public static Boolean isCharRegularChar(char a){
    return a != '\\' && a != '\'' && a != '\n' && a != '\r' && a != '\t';
  }

  public static List<TokenType> generateList(TokenType... tokenTypes) {
    List<TokenType> t=new ArrayList<>();
    Collections.addAll(t, tokenTypes);
    return t;
  }
}
