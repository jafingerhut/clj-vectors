# clj-vectors

A Clojure library that provides several different implementations of
Clojure persistent (i.e. immutable) vectors, intended primarily for
testing those implementations for correctness and/or performance.

The different implementations are intended only to differ in their
performance, e.g. some that implement [the RRB tree data
structure](https://github.com/clojure/core.rrb-vector/blob/master/doc/rrb-tree-notes.md)
claim to provide O(log N) worst case time concatenation of two vectors
of arbitrary size.  The different implementations are _not_ intended
to provide additional new capabilities other than faster run times for
some operations.

Some of them may have bugs that are independent of the other
implementations.  One of the reasons for creating this library is to
try to find those bugs using property based testing via
[`test.check`](https://github.com/clojure/test.check) and
[`collection-check`](https://github.com/ztellman/collection-check),
and perhaps additional ones developed within this repository.

`collection-check` expects the vector data structures it tests to
perform operations using Clojure functions like `clojure.core/pop`,
`clojure.core/conj`, etc.  One choice is to modify `collection-check`
to use the operations developed specifically for each unique vector
library.  This library instead implements the Java methods necessary
so that each vector library can be exercised using the Clojure
functions.

There are also many tests in the
[`core.rrb-vector`](https://github.com/clojure/core.rrb-vector)
library that manipulate vectors using Clojure functions, and it should
be possible to take advantage of those tests to exercise other vector
libraries using this repository.  Some of the test code in the
`core.rrb-vector` has already been adapted for use in this repository.


## Prerequisites

Install into your local `$HOME/.m2` modified versions of the Paguro
and Bifurcan libraries.  See comments of namespace clj-paguro-vector8
for why the modified version of the Paguro library was created.

```bash
$ git clone https://github.com/jafingerhut/Paguro
$ cd Paguro
$ git checkout andy-experiments-plus-proposed-fix-v1
$ ./mvn-build.sh
```

```bash
$ git clone https://github.com/jafingerhut/bifurcan
$ cd bifurcan
$ git checkout experimental-changes
$ lein install
```

I had success running the commands above on a system with Maven 3.6.2
and Leiningen 2.9.1 installed.  Most likely other versions will work,
too.


## Usage

To start REPL that is also listening for Clojure socket REPL
conections:
```bash
$ ./script/jdo
```

To run some tests that should be maintained so they stay relatively
short:
```bash
$ ./script/jdo test
```


## License

Copyright © 2019 Andy Fingerhut

This program and the accompanying materials are made available under
the terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
