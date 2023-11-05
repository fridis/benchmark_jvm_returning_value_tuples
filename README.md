# Benchmark for returning value tuples in Java

This is a set of micro-benchmarks to evaluate different implementation
approaches to return values of non-ref type features in the [Fuzion
Language](https://github.com/tokiwa-software/fuzion)'s JVM bytecode backend.

## Background

Java bytecode's product types are Java classes that are allocated on the heap
and accessed via a reference to an instance of the class.  Fuzion, in contrast,
has product types with value type semantics, i.e., there is no need for heap
allocation.

This poses a problem for the JVM backend for Fuzion since there is no direct
mechanism to return an instance of a value type, e.g., in the Fuzion code

```
   complex(re, im f64).

   add(a, b complex) => complex a.re+b.re a.im+b.im

   x := add (complex 1 0) (complex 0 1)
```

How should the backend return the tuple of two doubles from a call to `add`?
What we would need in Java is something like

```
  (double,double) add(a_re double, a_im double, b_re double, b_im double)
  {
    return (a_re + b_re, a_im + b_im);
  }

  (x_re, y_re) = add(1,0,0,1);
```

## Solutions

The following approaches should be analyzed

### case 0: Inlining

This is impractical for all cases but serves as a base case for benchmarking: If
a call is inlined, the result does not need to be returned, but it can be passed
in several local variable slots on the Java stack.

### case 1: using new

Wrapping the returned tuple into a new, heap allocated object and returning a
reference to the allocated instance.  The caller then immediately reads that
object's fields and drops the reference:

```
  class Temp
  {
    double re, im;
  }

  Temp add(a_re double, a_im double, b_re double, b_im double)
  {
    var res = new Temp();
    res.re = a_re + b_re;
    res.im = a_im + b_im;
    return res;
  }

  ...

  var tmp = add(1,0,0,1);
  x_re = tmp.re;
  x_im = tmp.im;
```

This seems painfully expensive thinking of the GC overhead poor locality of
memory accesses.

### case 2: using field in current thread

Since we have control over the threads that are started we can make sure that
the current thread instance is a subclass of `java.lang.Thread` that provides
fields to return structured values.

```
  class MyThread extends Thread
  {
    double add_re, add_im;
  }

  void add(a_re double, a_im double, b_re double, b_im double)
  {
    var ct = (MyThread) Thread.currentThread();
    ct.add_re = a_re + b_re;
    ct.add_im = a_im + b_im;
  }

  ...

  add(1,0,0,1);
  var ct = (MyThread) Thread.currentThread();
  x_re = ct.add_re;
  x_im = ct.add_im;
```

This largely depends on the performance of `Thread.currentThread()`.

### case 3: using static field

We could declare static fields and use them to return results

```
  static double add_re, add_im;

  void add(a_re double, a_im double, b_re double, b_im double)
  {
    add_re = a_re + b_re;
    add_im = a_im + b_im;
  }

  ...

  add(1,0,0,1);
  x_re = add_re;
  x_im = add_im;
```

This, however, is not thread-safe since there the same static fields would be
used by all threads using `add`.

### case 4: using thread locals variables

We could declare static fields and use them to return results

```
  static ThreadLocal<Double> add_re = new ThreadLocal<>();
  static ThreadLocal<Double> add_im = new ThreadLocal<>();

  void add(a_re double, a_im double, b_re double, b_im double)
  {
    add_re.set(a_re + b_re);
    add_im.set(a_im + b_im);
  }

  ...

  add(1,0,0,1);
  x_re = add_re.get();
  x_im = add_im.get();
```

This is thread safe, but the boxing of primitive types like `double` together
with the hash map used internally in Java's `ThreadLocal` seems prohibitively
expensive.

### case 5: using manual call-by-ref and static container

We could pass a ref to an instance to hold the result

```
  static class ResultContainer
  {
    double re, im;
  }
  static ResultContainer xc = new ResultContainer;

  void add(a_re double, a_im double, b_re double, b_im double, ResultContainer rc)
  {
    rc.re = a_re + b_re;
    rc.im = a_im + b_im;
  }

  ...
  var c = xc;
  add(1,0,0,1,c);
  x_re = c.get();
  x_im = c.get();
```

The problem here is that the container has to be stored somewhere, as static
field like here is not thread-safe. Alternatives are dynamic allocation or a
thread local, or to pass it around with all calls.

### case 6: using manual call-by-ref and dynamically allocated container

We could pass a ref to an instance to hold the result and allocate this instance whenever nee

```
  static class ResultContainer
  {
    double re, im;
  }

  void add(a_re double, a_im double, b_re double, b_im double, ResultContainer rc)
  {
    rc.re = a_re + b_re;
    rc.im = a_im + b_im;
  }

  ...
  var c = new ResultContainer();
  add(1,0,0,1,c);
  x_re = c.get();
  x_im = c.get();
```

## Benchmarks

There are two implementations, one using JMH and one using a hand-written simple
benchmark framework:

### Using JMH

Use make target `run` to get the throughput, `run_all` to measure all that JMH
can offer.

Limitations: I could not find a way to make JMH run using my own Java thread, so
case 2 is missing here.

### Using own benchmark framework

Use make target `run_directly` includes case 2, but does less sophisticated
measurements.
