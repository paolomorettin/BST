------
README
------

Compilation: javac *.java
Execution: java Main mode [params]

Modes / parameters:
- demo / graph_name min_value max_value
- sequentialdemo / graph_name min_value max_value
- random / iterations n_ops_per_thread n_threads min_value max_value
- correctness / iterations n_threads min_value max_value


Examples

1) Run a sequential demo with 10 insert, 5 delete and 5 find in a key space [-10,10[, saving the resulting tree (in DOT format) in the file prova.dot:
java Main sequentialdemo prova -10 10

2) Run a demo with 7 threads concurrently performing 10 insert, 5 delete and 5 find (not necessarily in this order), with random keys in [-10,10], saving the resulting tree (in DOT format) in the file example.dot (the standard output may be out of order wrt the operations performed):
java Main demo example -10 11

3) Run 60 iterations of a test with 10 threads, each one concurrently performing 9999 insertions or deletions of random keys in [0,10[, after each iteration it checks that the BST property is verified (see REPORT.pdf for the details):
java Main random 60 9999 10 0 10

4) Run 10 iterations of a test in which 7 threads concurrently insert or delete keys from a key space [4,8[. This is different from the "random" test as it also checks that the keys present in tree at the end are consistent with the operations performed (see REPORT.pdf for the details):
java Main correctness 10 7 4 8


