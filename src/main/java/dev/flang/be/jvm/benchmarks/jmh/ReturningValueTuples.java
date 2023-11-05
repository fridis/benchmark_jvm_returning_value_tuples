/**
 * Simple benchmark framework and collection of benchmarks for different
 * approaches to return value tuples from a method call in the JVM:
 */

package dev.flang.be.jvm.benchmarks.jmh;

import java.lang.management.ManagementFactory;

public class ReturningValueTuples extends Thread
{

  // help to determine gc count
  long gcCount()
  {
    long gcCount = 0;
    for(var gc : ManagementFactory.getGarbageCollectorMXBeans())
      {
        gcCount += Math.max(0, gc.getCollectionCount());
      }
    return gcCount;
  }

  // help to determine time spent in GC in milliseconds.
  long gcTime()
  {
    long gcTime = 0;
    for(var gc : ManagementFactory.getGarbageCollectorMXBeans())
      {
        gcTime += Math.max(0, gc.getCollectionTime());
      }
    return gcTime;
  }

  /**
   * functional interface to be used test to pass code to be benchmarked
   */
  static interface Subject
  {
    /**
     * The code to run, should run n times.
     */
    long f(long n);
  }

  /**
   * test the given code s
   *
   * @param name the name to be printed if show is set.
   *
   * @param s the code to test.
   *
   * @param min_time the minimum time to repeate s.
   *
   * @param show true to show the result, false for warmup runs.
   */
  void test(String name, Subject s, long min_time, boolean show)
  {
    long gcCount0 = gcCount();
    long gcTime0 = gcTime();

    var n = 256L;
    var total_time = 0L;
    var total_n = 0L;
    while (total_time < min_time)
      {
        var t = s.f(n);
        total_time += t;
        total_n += n;
        n = 2*n;
      }
    if (show)
      {
        long gcCount1 = gcCount();
        long gcTime1 = gcTime();

        System.out.println(String.format("%s: %7.3fns/it; GC: #%d %dms %2.2f%%; %d iter8ns",
                                         name,
                                         total_time * 1_000_000.0 / total_n,
                                         gcCount1 - gcCount0,
                                         gcTime1 - gcTime0,
                                         100.0 * (gcTime1 - gcTime0) / total_time,
                                         total_n));
      }
  }


  int case0_inlinedN(long n, int input)
  {
    var ra = input;
    var rb = input >>> 16 | input << 16;
    for (long i = 0; i<n; i++)
      {
        var a = ra;
        var b = rb;

        var a1 = (a << 7) | (a >>> 25) ^ b;
        var b1 = b ^ 0xaaaa5555;

        var ta = a1;
        var tb = b1;
        ra ^= tb;
        rb ^= ta;
      }
    return ra ^ rb;
  }


  long case0_inlined(long n)
  {
    var start = System.currentTimeMillis();
    var res = case0_inlinedN(n, produce());
    consume(res);
    var end = System.currentTimeMillis();
    return end - start;
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


  int case1_usingNewN(long n, int input)
  {
    var ra = input;
    var rb = input >>> 16 | input << 16;
    for (long i = 0; i<n; i++)
      {
        var t = case1_usingNew(ra, rb);
        var ta = t.a;
        var tb = t.b;
        ra ^= tb;
        rb ^= ta;
      }
    return ra ^ rb;
  }


  long case1_usingNew(long n)
  {
    var start = System.currentTimeMillis();
    var res = case1_usingNewN(n, produce());
    consume(res);
    var end = System.currentTimeMillis();
    return end - start;
  }




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

  int case2_usingCurrentThreadN(long n, int input)
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


  long case2_usingCurrentThread(long n)
  {
    var start = System.currentTimeMillis();
    var res = case2_usingCurrentThreadN(n, produce());
    consume(res);
    var end = System.currentTimeMillis();
    return end - start;
  }



  static int global_a;
  static int global_b;

  void case3_usingStatic(int a, int b)
  {
    var a1 = (a << 7) | (a >>> 25) ^ b;
    var b1 = b ^ 0xaaaa5555;
    var ct = (ReturningValueTuples) Thread.currentThread();
    global_a = a1;
    global_b = b1;
  }

  int case3_usingStaticN(long n, int input)
  {
    var ra = input;
    var rb = input >>> 16 | input << 16;
    for (long i = 0; i<n; i++)
      {
        case3_usingStatic(ra, rb);
        var ta = global_a;
        var tb = global_b;
        ra ^= tb;
        rb ^= ta;
      }
    return ra ^ rb;
  }


  long case3_usingStatic(long n)
  {
    var start = System.currentTimeMillis();
    var res = case3_usingStaticN(n, produce());
    consume(res);
    var end = System.currentTimeMillis();
    return end - start;
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

  int case4_usingThreadLocalN(long n, int input)
  {
    var ra = input;
    var rb = input >>> 16 | input << 16;
    for (long i = 0; i<n; i++)
      {
        case4_usingThreadLocal(ra, rb);
        var ta = threadLocal_a.get();
        var tb = threadLocal_b.get();
        ra ^= tb;
        rb ^= ta;
      }
    return ra ^ rb;
  }


  long case4_usingThreadLocal(long n)
  {
    var start = System.currentTimeMillis();
    var res = case4_usingThreadLocalN(n, produce());
    consume(res);
    var end = System.currentTimeMillis();
    return end - start;
  }


  static class Case5Container
  {
    int a, b;
  }


  void case5_passingContainer(int a, int b, Case5Container c)
  {
    var a1 = (a << 7) | (a >>> 25) ^ b;
    var b1 = b ^ 0xaaaa5555;
    c.a = a1;
    c.b = b1;
  }

  int case5_passingContainerN(long n, int input)
  {
    var c = new Case5Container();
    var ra = input;
    var rb = input >>> 16 | input << 16;
    for (long i = 0; i<n; i++)
      {
        case5_passingContainer(ra, rb, c);
        var ta = c.a;
        var tb = c.b;
        ra ^= tb;
        rb ^= ta;
      }
    return ra ^ rb;
  }


  long case5_passingContainer(long n)
  {
    var start = System.currentTimeMillis();
    var res = case5_passingContainerN(n, produce());
    consume(res);
    var end = System.currentTimeMillis();
    return end - start;
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

  int case6_passingDynAllocContainerN(long n, int input)
  {
    var c = new Case6DynAllocContainer();
    var ra = input;
    var rb = input >>> 16 | input << 16;
    for (long i = 0; i<n; i++)
      {
        case6_passingDynAllocContainer(ra, rb, c);
        var ta = c.a;
        var tb = c.b;
        ra ^= tb;
        rb ^= ta;
      }
    return ra ^ rb;
  }


  long case6_passingDynAllocContainer(long n)
  {
    var start = System.currentTimeMillis();
    var res = case6_passingDynAllocContainerN(n, produce());
    consume(res);
    var end = System.currentTimeMillis();
    return end - start;
  }



  int _sourceAndSink = (int) System.currentTimeMillis();

  int produce()
  {
    return _sourceAndSink;
  }
  void consume(int v)
  {
    _sourceAndSink ^= v;
  }


  public static void main(String[] args)
  {
    new ReturningValueTuples().start();
  }

  public void runAll(long t, boolean show)
  {
    test("base case: inline:                   ", n->case0_inlined                 (n), t, show);
    test("using new:                           ", n->case1_usingNew                (n), t, show);
    test("using currentThread:                 ", n->case2_usingCurrentThread      (n), t, show);
    test("using static vars (not thread-safe): ", n->case3_usingStatic             (n), t, show);
    test("using ThreadLocal:                   ", n->case4_usingThreadLocal        (n), t, show);
    test("using Container passed as extra arg  ", n->case5_passingContainer        (n), t, show);
    test("using dyn alloc Container extra arg  ", n->case6_passingDynAllocContainer(n), t, show);
  }
  public void run()
  {
    System.out.println("warmup...");
    runAll(500, false);
    System.out.println("testing...");
    runAll(3000, true);
    if (System.currentTimeMillis() > System.currentTimeMillis())
      System.out.println("ignore this: using result to avoid compiler optimizations: "+_sourceAndSink);
  }


}
