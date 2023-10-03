package intraprocedural;

import soot.*;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class MainDriver {
    public static void main(String[] args) {
        IntraTaintTransformer transformer = new IntraTaintTransformer();

        /* Process the input arguments from the command line. */
        if (args == null || args.length < 3) {
            System.out.println("Please specify the absolute path of the project's root folder, the sink filename, and the source filename (in order)");
            return;
        }
        String processDir = args[0];
        transformer.setSinkFilename(args[0] + File.separator + args[1]);
        transformer.setSourceFilename(args[0] + File.separator + args[2]);
//        String processDir = "/Users/liliwei/Documents/0WorkFolder/ShareBetweenMacs/McGill/Teaching/2022F/ECSE688/PA1/TestPrograms/";


        // Set soot classpath
        Options.v().set_soot_classpath(Scene.v().defaultClassPath());

        // Add our custom taint analysis to jtp phase of Soot
        Pack jtp = PackManager.v().getPack("jtp");
        jtp.add(new Transform("jtp.instrumenter",
                transformer));

        // Arguments to run Soot. A list of available soot parameters: https://www.sable.mcgill.ca/soot/tutorial/usage/
        String[] sootArgs = new String[] {
                "-p", "jb", "use-original-names:true",
                "-process-dir",
                processDir
        };
        Options.v().set_keep_line_number(true);
        soot.Main.v().run(sootArgs);
    }
}

// The Transformer class as a hook to add the Taint analysis to the jtp phase of Soot
class IntraTaintTransformer extends BodyTransformer {
    private String sinkFilename;
    private String sourceFilename;

    public void setSinkFilename(String sinkFilename) {
        this.sinkFilename = sinkFilename;
    }

    public void setSourceFilename(String sourceFilename) {
        this.sourceFilename = sourceFilename;
    }

    @Override
    protected void internalTransform(Body body, String s, Map<String, String> map) {
        // Build the unit graph for the analyzed method
        UnitGraph unitGraph = new BriefUnitGraph(body);

        //Construct an instance of Taint Analysis and conduct the analysis
        try {
            new TaintAnalysis(unitGraph, sinkFilename, sourceFilename, body.getMethod().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
