import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class App {
    public static void main(String[] args) throws IOException, CompileError {
        try {
            InputStream input;
            PrintStream output;
            DataOutputStream out;
            System.out.println(args[1]);
            String inputFileName = args[1], outputFileName = args[2];
            input = new FileInputStream(new File(inputFileName));
            out = new DataOutputStream(new FileOutputStream(new File(outputFileName)));
            Scanner scanner;
            scanner = new Scanner(input);
            StringIter iter = new StringIter(scanner);
            Tokenizer tokenizer = tokenize(iter);
            Analyser analyzer = new Analyser(tokenizer);
            analyzer.analyse();

            OutToBinary outPutBinary=new OutToBinary(analyzer.def_table, analyzer.getStartFunction());
            List<Byte> bs=outPutBinary.generate();
            byte[] temp=new byte[bs.size()];
            for(int i=0;i<bs.size();i++)
              temp[i]=bs.get(i);
            out.write(temp);
        }catch (Exception e){
            System.exit(-1);
        }
    }

    private static Tokenizer tokenize(StringIter iter) {
        return new Tokenizer(iter);
    }
}
