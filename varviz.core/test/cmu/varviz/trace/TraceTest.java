package cmu.varviz.trace;

import java.io.PrintWriter;

import org.junit.Test;

import cmu.vaviz.testutils.TraceFactory;

public class TraceTest {

	@Test
	public void traceTest() {
		PrintWriter pw = new PrintWriter(System.out);
		TraceFactory.createTrace().printToGraphViz(pw);
	}

}
