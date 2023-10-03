package intraprocedural;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import soot.Pack;
import soot.PackManager;
import soot.Scene;
import soot.Transform;
import soot.options.Options;

import java.io.File;
import java.util.List;
import java.util.Objects;

class MainDriverTest {

    private static IntraTaintTransformer t;
    private static Pack jtp;

    @BeforeEach
    void setUp() {
        Options.v().set_soot_classpath(Scene.v().defaultClassPath());
        Options.v().set_keep_line_number(true);
        jtp = PackManager.v().getPack("jtp");
        t = new IntraTaintTransformer();
    }

    @ParameterizedTest
    @CsvSource({"src/test/resources/TestCases/ImplicitFlow1,location,1,srcStr,2,snkStr",
                "src/test/resources/TestCases/ImplicitFlow2,location,1,srcStr,2,snkStr",
                "src/test/resources/TestCases/MultiTaint1,location,1,srcStr,2,snkStr",
                "src/test/resources/TestCases/MultiTaint2,<ProgramToAnalyzeWithExpectedOutputs: void multiTaint2()>,100,a = staticinvoke <sensitive.Source: int source()>(),103,staticinvoke <sensitive.Sink: void leak(int)>(c)",
                "src/test/resources/TestCases/MultiTaint3,location,1,srcStr,2,snkStr",
                "src/test/resources/TestCases/MultiTaintImplicitFlow,location,1,srcStr,2,snkStr",
                "src/test/resources/TestCases/SimpleFlow1,<ProgramToAnalyzeWithExpectedOutputs: void simpleFlow1()>,14,x = staticinvoke <sensitive.Source: int source()>(),17,staticinvoke <sensitive.Sink: void sink(int)>(z)",
                "src/test/resources/TestCases/SimpleFlow2,<ProgramToAnalyzeWithExpectedOutputs: void simpleFlow2()>,28,a = staticinvoke <sensitive.Source: int sensitiveInfo()>(),32,staticinvoke <sensitive.Sink: void leak(int)>(c)",
                "src/test/resources/TestCases/SimpleFlow3,<ProgramToAnalyzeWithExpectedOutputs: void simpleFlow3()>,44,a = staticinvoke <sensitive.Source: int sensitiveInfo()>(),48,staticinvoke <sensitive.Sink: void leak(int)>(a)",
                "src/test/resources/TestCases/LongTaintChain,<ProgramToAnalyzeWithExpectedOutputs: void longTaintChain()>,60,a = staticinvoke <sensitive.Source: int sensitiveInfo()>(),65,staticinvoke <sensitive.Sink: void leak(int)>(e)"})
    void testProgramToAnalyzeWithExpectedOutputs_OneLeak(String path, String location, int sourceLine, String source, int sinkLine, String sink) {
        runSoot(path);
        TaintAnalysis taintAnalysis = t.getAnyTaintAnalysisWithLeaks();
        Assumptions.assumeFalse(Objects.isNull(taintAnalysis));
        List<Leak> leaks = taintAnalysis.getLeaks();
        Assumptions.assumeTrue(leaks.size() == 1, "Expected 1 leak, but got " + leaks.size() + " leaks");
        Assertions.assertEquals(new Leak(location, sourceLine, source, sinkLine, sink), leaks.get(0));
    }

    @ParameterizedTest
    @CsvSource({"src/test/resources/TestCases/ImplicitFlow1,0",
            "src/test/resources/TestCases/ImplicitFlow2,0",
            "src/test/resources/TestCases/MultiTaint1,2",
            "src/test/resources/TestCases/MultiTaint2,1",
            "src/test/resources/TestCases/MultiTaint3,2",
            "src/test/resources/TestCases/MultiTaintImplicitFlow,0",
            "src/test/resources/TestCases/SimpleFlow1,1",
            "src/test/resources/TestCases/SimpleFlow2,1",
            "src/test/resources/TestCases/SimpleFlow3,1",
            "src/test/resources/TestCases/LongTaintChain,1"})
    void testProgramToAnalyzeWithExpectedOutputs_LeaksCount(String path, int leaksCount) {
        runSoot(path);
        TaintAnalysis taintAnalysis = t.getAnyTaintAnalysisWithLeaks();
        Assumptions.assumeFalse(Objects.isNull(taintAnalysis));
        List<Leak> leaks = taintAnalysis.getLeaks();
        Assertions.assertEquals(leaksCount, leaks.size());
    }

    @ParameterizedTest
    @CsvSource({"src/test/resources/TestCases/ImplicitFlow1,0",
            "src/test/resources/TestCases/ImplicitFlow2,0",
            "src/test/resources/TestCases/MultiTaintImplicitFlow,0",})
    void testProgramToAnalyzeWithExpectedOutputs_ImplicitFlow(String path, int leaksCount) {
        runSoot(path);
        TaintAnalysis taintAnalysis = t.getAnyTaintAnalysisWithLeaks();
        Assertions.assertNotNull(taintAnalysis);
        List<Leak> leaks = taintAnalysis.getLeaks();
        Assertions.assertEquals(leaksCount, leaks.size());
    }

    private static void runSoot(String path) {
        t.setSinkFilename(path + File.separator + "Sink.txt");
        t.setSourceFilename(path + File.separator + "Source.txt");
        jtp.add(new Transform("jtp.instrumenter", t));
        soot.Main.v().run(new String[] {
                "-p", "jb", "use-original-names:true",
                "-process-dir",
                path
        });
    }

    @AfterEach
    void tearDown() {
        t.getTaintAnalyses().clear();
        jtp.remove("jtp.instrumenter");
        soot.G.reset();
    }
}