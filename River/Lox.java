package River;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;



public class Lox {
    static boolean hadError = false;

    private static void report(int lineNum, String location, String msg){
        System.err.println("lineNum " + lineNum);
        System.err.println("location" + location);
        System.err.println("message" + msg);
        hadError = true;
    }

    static void error(int lineNum, String msg){
        report(lineNum,"",msg);
    }

    

    public static void main(String[] args) throws IOException {
        
        if (args.length == 1){
            runFile(args[0]);
        }
        else if (args.length > 1) {
            System.out.println("use river's script");
            System.exit(64);
        }
        else {
            runPrompt();
        }
        
    }

    private static void run(String sourcing){
        Scanner scanner = new Scanner(sourcing);
        List<Tokens> tokens = scanner.scanTokens();

        for (Tokens IndivToken : tokens) {
            System.out.println(IndivToken);
        }

    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError) System.exit(65);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for(;;){

            System.out.print(">");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false; 

        }
    }
}