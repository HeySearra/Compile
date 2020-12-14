import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

  public static long StringToLong(String a){
    long aws = 0;
    long xi = 1;
    for(int i=a.length()-1; i>=0; i--){
      if(a.charAt(i) == '1')
        aws += xi;
      xi *=2;
    }
    return aws;
  }
}
