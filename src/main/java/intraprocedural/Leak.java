package intraprocedural;

import com.google.common.annotations.VisibleForTesting;
import soot.Unit;

@VisibleForTesting
public class Leak {

    private String location;
    private int sourceLine;
    private String source;
    private int sinkLine;
    private String sink;

    public Leak(String location, int sourceLine, String source, int sinkLine, String sink) {
        this.location = location;
        this.sourceLine = sourceLine;
        this.source = source;
        this.sinkLine = sinkLine;
        this.sink = sink;
    }

    public static Leak createLeak(Unit source, Unit sink, String location) {
        return new Leak(location, source.getJavaSourceStartLineNumber(), source.toString(), sink.getJavaSourceStartLineNumber(), sink.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Leak leak = (Leak) o;

        if (sourceLine != leak.sourceLine) return false;
        if (sinkLine != leak.sinkLine) return false;
        if (!location.equals(leak.location)) return false;
        if (!source.equals(leak.source)) return false;
        return sink.equals(leak.sink);
    }

    @Override
    public String toString() {
        return "Leak{" +
                "location='" + location + '\'' +
                ", sourceLine=" + sourceLine +
                ", source='" + source + '\'' +
                ", sinkLine=" + sinkLine +
                ", sink='" + sink + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        int result = location.hashCode();
        result = 31 * result + sourceLine;
        result = 31 * result + source.hashCode();
        result = 31 * result + sinkLine;
        result = 31 * result + sink.hashCode();
        return result;
    }
}
