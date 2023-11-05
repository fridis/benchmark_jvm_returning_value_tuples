/**
 * JMH benchmarks for different approaches to return value tuples from a method
 * call in the JVM:
 */
package dev.flang.be.jvm.benchmarks.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Thread)
public class ReturningValueTuplesCases
{

  private int xa = 0x12345678;
  private int xb = 0x9abcdef0;


  public void case0_inline(int ra, int rb, Blackhole bh)
  {
    var a = ra;
    var b = rb;
    var a1 = (a << 7) | (a >>> 25) ^ b;
    var b1 = b ^ 0xaaaa5555;

    var ta = a1;
    var tb = b1;
    ra ^= tb;
    rb ^= ta;
    bh.consume(ra);
    bh.consume(rb);
  }

  @Benchmark
  public void case0_inline(Blackhole bh) {
    case0_inline(xa, xb, bh);
  }


  static class Wrapper
  {
    int a,b;
    Wrapper(int a, int b)
    {
      this.a = a;
      this.b = b;
    }
  }

  Wrapper case1_usingNew(int a, int b)
  {
    if (a == b) return case1_usingNew(a+1,b-1);
    var a1 = (a << 7) | (a >>> 25) ^ b;
    var b1 = b ^ 0xaaaa5555;
    return new Wrapper(a1, b1);
  }

  @Benchmark
  public void case1_usingNew(Blackhole bh) {
    var w = case1_usingNew(xa, xb);
    bh.consume(w.a);
    bh.consume(w.b);
  }


  /* NYI: case4, using currentThread(): how can this be done using JMH?

  int thread_local_a;
  int thread_local_b;

  void case2_usingCurrentThread(int a, int b)
  {
    var a1 = (a << 7) | (a >>> 25) ^ b;
    var b1 = b ^ 0xaaaa5555;
    var ct = (ReturningValueTuples) Thread.currentThread();
    ct.thread_local_a = a1;
    ct.thread_local_b = b1;
  }

  int case2_usingCurrentThread(long n, int input)
  {
    var ra = input;
    var rb = input >>> 16 | input << 16;
    for (long i = 0; i<n; i++)
      {
        case2_usingCurrentThread(ra, rb);
        var ct = (ReturningValueTuples) Thread.currentThread();
        var ta = ct.thread_local_a;
        var tb = ct.thread_local_b;
        ra ^= tb;
        rb ^= ta;
      }
    return ra ^ rb;
  }

   */


  static int global_a;
  static int global_b;

  void case3_usingStatic(int a, int b)
  {
    var a1 = (a << 7) | (a >>> 25) ^ b;
    var b1 = b ^ 0xaaaa5555;
    global_a = a1;
    global_b = b1;
  }

  @Benchmark
  public void case3_usingStatic(Blackhole bh)
  {
    case3_usingStatic(xa, xb);
    var ta = global_a;
    var tb = global_b;
    bh.consume(ta);
    bh.consume(tb);
  }



  static ThreadLocal<Integer> threadLocal_a = new ThreadLocal<Integer>();
  static ThreadLocal<Integer> threadLocal_b = new ThreadLocal<Integer>();

  void case4_usingThreadLocal(int a, int b)
  {
    var a1 = (a << 7) | (a >>> 25) ^ b;
    var b1 = b ^ 0xaaaa5555;
    threadLocal_a.set(a1);
    threadLocal_b.set(b1);
  }

  @Benchmark
  public void case4_usingThreadLocal(Blackhole bh)
  {
    case4_usingThreadLocal(xa, xb);
    var ta = threadLocal_a.get();
    var tb = threadLocal_b.get();
    bh.consume(ta);
    bh.consume(tb);
  }



  static class Case5Container
  {
    int a, b;
  }
  Case5Container xc = new Case5Container();


  void case5_passingContainer(int a, int b, Case5Container c)
  {
    var a1 = (a << 7) | (a >>> 25) ^ b;
    var b1 = b ^ 0xaaaa5555;
    c.a = a1;
    c.b = b1;
  }

  @Benchmark
  public void case5_passingContainer(Blackhole bh)
  {
    var c = xc;
    case5_passingContainer(xa, xb, c);
    var ta = c.a;
    var tb = c.b;
    bh.consume(ta);
    bh.consume(tb);
  }


  static class Case6DynAllocContainer
  {
    int a, b;
  }


  void case6_passingDynAllocContainer(int a, int b, Case6DynAllocContainer c)
  {
    var a1 = (a << 7) | (a >>> 25) ^ b;
    var b1 = b ^ 0xaaaa5555;
    c.a = a1;
    c.b = b1;
  }

  @Benchmark
  public void case6_passingDynAllocContainer(Blackhole bh)
  {
    var c = new Case6DynAllocContainer();
    case6_passingDynAllocContainer(xa, xb, c);
    var ta = c.a;
    var tb = c.b;
    bh.consume(ta);
    bh.consume(tb);
  }

}
