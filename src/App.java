import analyser.Analyser;
import error.CompileError;
import instruction.Instruction;
import instruction.OutToBinary;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import tokenizer.StringIter;
import tokenizer.Token;
import tokenizer.TokenType;
import tokenizer.Tokenizer;


public class App {
    public static void main(String[] args) throws FileNotFoundException, CompileError {
        InputStream input;
        PrintStream output;
        DataOutputStream out;
        System.out.println(args[1]);
        String inputFileName=args[1],outputFileName=args[2];
        input = new FileInputStream(new File(inputFileName));
        output = new PrintStream(new FileOutputStream(new File(outputFileName)));
        out=new DataOutputStream(new FileOutputStream(new File(outputFileName)));
        Scanner scanner;
        scanner = new Scanner(input);
        StringIter iter = new StringIter(scanner);
        Tokenizer tokenizer = tokenize(iter);
        Analyser analyzer = new Analyser(tokenizer);

        System.out.println("\n------------------Analyser Start");
        analyzer.analyse();

        System.out.println("\n----------------------------生成二进制");
//            OutToBinary binary = new OutToBinary(Analyser.getGlobals(), Analyser.getStartFunction(), Analyser.getFunctionDefs());
//
//            DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(args[1])));
//            List<Byte> bytes = binary.generate();
//            byte[] resultBytes = new byte[bytes.size()];
//            for (int i = 0; i < bytes.size(); ++i) {
//                resultBytes[i] = bytes.get(i);
//            }
//            out.write(resultBytes);
    }

    private static Tokenizer tokenize(StringIter iter) {
        return new Tokenizer(iter);
    }
}
