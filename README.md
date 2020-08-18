Implemented a program that simulates the behavior of the two-phase locking (2PL) protocol for concurrency control. The particular protocol to be implemented will be rigorous 2PL, with the wound-wait method for dealing with deadlock. The input to the program will be a file of transaction operations in a particular sequence. Each line has a single transaction operation. The possible operations are b (begin transaction), r (read item), w (write item), and e (end transaction). Each operation will be followed by a transaction id that is an integer between 1 and 9.

For r and w operations, an item name follows between parentheses (item names are single letters from A to Z). Two examples are given below.

Examples of two input files:

b1;                                                                        b1;

r1 (Y);                                                                    r1(Y);

w1 (Y);                                                                    w1(Y);

r1 (Z);                                                                    r1(Z);

b3;                                                                        b2;

r3 (X);                                                                    r2(Y);

w3 (X);                                                                    b3;

w1 (Z);                                                                    r3(Z);

e1;                                                                        w1(Z);

r3 (Y);                                                                    e1;

b2;                                                                        w3(Z);

r2 (Z);                                                                    e3;

w2 (Z);                                                                  

w3 (Y);

e3;

r2 (X);

w2 (X);

e2;

Designed and implemented appropriate data structures to keep track of transactions (transaction table) and locks (lock table), as well as any other needed information (it is your responsibility as part of the project to determine any additional needed information).

In the transaction table, kept relevant information about each transaction. This includes transaction id, transaction timestamp, transaction state (active, blocked (waiting), aborted (canceled), committed, etc.), list of items locked by the transaction, plus any other relevant information.  For blocked transactions, kept an ordered list of the operations of that transaction (from the input file) that are waiting to be executed if the transaction can be resumed.

In the lock table, kept relevant information about each locked data item. This includes the item name, lock state (read (share) locked, or write (exclusive) locked), transaction id for the transaction holding the lock (for write-locked) or list of transaction ids for the transactions holding the lock (for reading locked), list of transaction ids for transactions waiting for the item to be unlocked (if any), plus any other relevant information. (It is part of your work to determine and specify other relevant information.)

The program reads the operations from an input file representing a schedule and simulates the appropriate actions for each operation by referring to and updating the entries in the transaction table and lock table. Printed a summary of the simulation action that the program takes to simulate each command, including information on any updates to the system tables (transaction table and lock table), and if the simulation will commit or abort or block a transaction, or just allow the operation to execute.
 

Some basic information about the actions that the program has:

A transaction record is created in the transaction table when a begin transaction operation (b) is processed. The state of this transaction is set to active when it begins. A transaction timestamp is created for the transaction and stored with the transaction record.

Before processing a read operation (r(X)), the appropriate read lock(X) request is simulated by the program simulation, and the lock table should be updated appropriately. If the item is not locked, the operation is allowed and the lock table is updated to indicate that the requested item is now read-locked by the transaction.

If the item X is already locked by a conflicting write lock, the transaction is either: (i) blocked (wait) and its transaction state is changed to blocked (in the transaction table), or (ii) the simulation will abort the other transaction holding the write lock on item X (wound) and the state of that other transaction is changed to aborted. The deadlock prevention protocol (wound-wait) determines which action is taken based on the transaction timestamps.

If the item is already locked by a non-conflicting read lock, the transaction is added to the list of transactions that hold the read lock on the requested item (in the lock table) and is not blocked.

Before processing a write operation (w(X)), the appropriate write lock(X) request is processed by the program simulation (lock upgrading is permitted if the upgrade conditions are met – that is if the item is read locked by only the transaction that is requesting the write lock). The lock table is updated appropriately.

If the item is not locked, the operation is allowed and the lock table is updated to indicate that the requested item is now write-locked by the transaction.

If the item is already locked by a conflicting read or write lock, the transaction is either: (i) blocked (wait) and its state is changed to blocked (in the transaction table), or (ii) the simulation will abort the other transaction holding the read or write lock on item X (wound) and the state of that other transaction is changed to aborted. The deadlock prevention protocol (wound-wait) determines which action is taken based on the transaction timestamps.

The simulation program includes code for each operation needed. 

Before processing operation in the input list, the program checks if the transaction is in a blocked state or aborted state. (i) If it is blocked, added the operation to the ordered list of the operations of that transaction that are waiting to be executed (in the transaction table); these operations will be executed if the transaction is resumed, subject to the rules of the locking protocol. (ii) If the transaction has already been aborted, its subsequent operations in the input list are ignored.

Before changing a transaction state to blocked, the program checks the deadlock prevention protocol (wound-wait) rules to determine if the transaction should wait (be blocked) or abort the other transaction (wound). The transaction timestamps are used to decide on which action to take.

The process of aborting a transaction should release (unlock) any items that are currently locked by the transaction, one at a time, and changing the transaction state to aborted in the transaction table. Any subsequent operations of the aborted transaction that are read from the input file should be ignored by the simulation.

If a transaction reaches its end (e) operation successfully, it is committed. The process of committing a transaction release (unlock) any items that are currently locked by the transaction, one at a time, and changing the transaction state to committed in the transaction table.

The process of unlocking an item checks if any transaction(s) are blocked because of waiting for the item. If any transactions are waiting, it removes the first waiting transaction and grant it access to the item (note that this will relock the item) and thus resume the transaction. All waiting operations of the transaction are processed (as discussed above, subject to the locking protocol) before any further operations from the input file are processed.
