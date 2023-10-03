package intraprocedural;

import com.google.common.annotations.VisibleForTesting;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.ValueBox;
import soot.jimple.Stmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import soot.util.Chain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * The class extending ForwardFlowAnalysis. This is where you need to implement your taint analysis
 */
public class TaintAnalysis extends ForwardFlowAnalysis<Unit, FlowMap<Unit, Value>> {

    private final List<String> sinks;
    private final List<String> sources;
    private final List<Leak> leaks = new ArrayList<>();

    //Constructor of the class
    public TaintAnalysis(DirectedGraph<Unit> graph, String sinkFile, String sourceFile, String location) throws IOException {
        super(graph);

        // Read the sink and source files
        sinks = Files.readAllLines(Paths.get(sinkFile)).stream().map(String::trim).collect(Collectors.toList());
        sources = Files.readAllLines(Paths.get(sourceFile)).stream().map(String::trim).collect(Collectors.toList());

        //doAnalysis will perform the analysis
        doAnalysis();

        // Print out the analysis results
        UnitGraph unitGraph = (UnitGraph) graph;

        // The unit chain that can iterate over all the units in the unit graph
        Chain<Unit> unitChain = unitGraph.getBody().getUnits();
        for (Unit unit : unitChain) {
            // Get the IN state of unit after the analysis
            FlowMap<Unit, Value> inState = this.getFlowBefore(unit);

            // Cast the unit to Stmt (statement)
            Stmt stmt = (Stmt) unit;

            // Check if unit is a sink
            for (String line : sinks) {
                if (isInvocationOf(stmt, line)) {
                    Set<Value> usedValues = getValuesUsedIn(unit);

                    // Check whether any of the used variables are tainted.
                    for (Map.Entry<Unit, FlowSet<Value>> entry : inState.entrySet()) {
                        Unit source = entry.getKey();
                        FlowSet<Value> taintedValues = entry.getValue();
                        //If a variable is tainted, report a leak
                        for (Value value : taintedValues) {
                            if (usedValues.contains(value)) {
                                leaks.add(Leak.createLeak(source, unit, location));
                                System.out.println("——————————————————");
                                System.out.println("Found a leak in " + location);
                                System.out.println("Source: line " + source.getJavaSourceStartLineNumber() + ": " + source);
                                System.out.println("Sink: line " + unit.getJavaSourceStartLineNumber() + ": " + unit);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    @VisibleForTesting
    public List<Leak> getLeaks() {
        return leaks;
    }

    private static Set<Value> getValuesUsedIn(Unit unit) {
        Set<Value> usedValues = new HashSet<>();
        for (ValueBox usedValueBoxes : unit.getUseBoxes()) {
            usedValues.add(usedValueBoxes.getValue());
        }
        return usedValues;
    }

    private static boolean isInvocationOf(Stmt stmt, String signature) {
        return (stmt.containsInvokeExpr() &&
                stmt.getInvokeExpr().getMethodRef().
                        getSignature().contains(signature.trim())) || stmt.getUnitBoxes().stream().map(UnitBox::getUnit).anyMatch(unit->isInvocationOf((Stmt) unit, signature));
//        return stmt.toString().contains(signature.trim());
    }


    @Override
    protected void flowThrough(FlowMap<Unit, Value> inState, Unit unit, FlowMap<Unit, Value> outState) {
        /*
        * TODO: handle implicit flows
        * */
        Stmt stmt = (Stmt) unit;
        copy(inState, outState);
        if (matchSources(outState, stmt) == null && findUsedTaintedValue(stmt, inState, outState) == null) {
            untaintValues(outState, stmt);
        }
    }

    private Unit findUsedTaintedValue(Unit stmt, FlowMap<Unit, Value> inState, FlowMap<Unit, Value> outState) {
        for (Value usedValue : getValuesUsedIn(stmt)) {
            Unit srcStmt;
            if ((srcStmt = inState.getContainingSet(usedValue)) != null) {
                taintValues(outState, stmt, srcStmt);
                return srcStmt;
            }
        }
        return null;
    }

    private Unit matchSources(FlowMap<Unit, Value> outState, Stmt stmt) {
        for (String src : sources) {
            if (isInvocationOf(stmt, src)) {
                taintValues(outState, stmt);
                return stmt;
            }
        }
        return null;
    }

    private static void taintValues(FlowMap<Unit, Value> outState, Unit stmt) {
        taintValues(outState, stmt, stmt);
    }

    private static void taintValues(FlowMap<Unit, Value> outState, Unit affected, Unit src) {
        for (Value definedValue : getValuesDefinedIn(affected)) {
            outState.add(src, definedValue);
        }
    }

    private static void untaintValues(FlowMap<Unit, Value> outState, Unit stmt) {
        for (Value definedValue : getValuesDefinedIn(stmt)) {
            outState.removeValue(stmt, definedValue);
        }
    }

    private static Set<Value> getValuesDefinedIn(Unit stmt) {
        Set<Value> definedValues = new HashSet<>();
        for (ValueBox definedVars : stmt.getDefBoxes()) {
            definedValues.add(definedVars.getValue());
        }
        return definedValues;
    }

    @Override
    protected FlowMap<Unit, Value> newInitialFlow() {
        // Initialize each program state
        return new FlowMap<>();
    }

    @Override
    protected void merge(FlowMap<Unit, Value> out1, FlowMap<Unit, Value> out2, FlowMap<Unit, Value> in) {
        // Merge program state out1 and out2 into in
        out1.union(out2, in);
    }

    @Override
    protected void copy(FlowMap<Unit, Value> src, FlowMap<Unit, Value> dest) {
        // Copy from src to dest
        src.copy(dest);
    }

    @Override
    protected FlowMap<Unit, Value> entryInitialFlow() {
        // Initialize the initial program state
        return new FlowMap<>();
    }
}