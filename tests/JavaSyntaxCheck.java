import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.util.*;
/** Parses actual source with JDK 21. Deliberately does not resolve Bukkit types. */
public class JavaSyntaxCheck {
    public static void main(String[] args) throws Exception {
        var compiler=ToolProvider.getSystemJavaCompiler();
        var diagnostics=new DiagnosticCollector<JavaFileObject>();
        try(var files=compiler.getStandardFileManager(diagnostics,null,null)) {
            var units=files.getJavaFileObjects(args);
            var task=(JavacTask)compiler.getTask(null,files,diagnostics,List.of("--release","21","-proc:none"),null,units);
            int count=0;for(var tree:task.parse()) count++;
            for(var diagnostic:diagnostics.getDiagnostics()) if(diagnostic.getKind()==Diagnostic.Kind.ERROR)
                throw new AssertionError(diagnostic.toString());
            System.out.println("PASS: JDK parsed "+count+" Java files. Syntax only; NOT Folia API type checking or execution.");
        }
    }
}
