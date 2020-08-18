import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class ConcurrencyControl {
    static FileWriter fw;
    public static void main(String args[]) throws IOException {
        /*Reading input file through BufferReader's object*/
        BufferedReader br = new BufferedReader(new FileReader("sample.txt"));
        String st;
        /*Created LinkedHashMaps for transactions and locks for keeping the records of both*/
        HashMap<String, HashMap<String, Object>> transactions = new LinkedHashMap<>();
        HashMap<Character, HashMap<String, Object>> locks = new LinkedHashMap<>();

        /*Created HashMap for keeping the records for blocked operation with respect to the transactionId which blocks those operation*/
        HashMap<String, LinkedList<String>> blockedOp = new HashMap<>();
        int i = 1;
        ConcurrencyControl cc = new ConcurrencyControl();
        fw = new FileWriter("output.txt");
        while ((st = br.readLine()) != null) {
            st = st.trim();
            int index = st.indexOf(';');
            fw.write("Input : "+ st.substring(0, index) +"\n");
            if (st.charAt(0) == 'b') {
                /* will just create record of current transaction with active state and timestamp */
                cc.beginTransaction(transactions, locks, st, i);
                i++;
            }
            else if (st.charAt(0) == 'r') {
                /* will perform read operation */
               cc.readTransaction(transactions, locks, st, blockedOp);
            }
            else if (st.charAt(0) == 'w') {
                /* will perform read operation */
                cc.writeTransaction(transactions, locks, st, blockedOp);
            }
            else {
                String key = "T"+st.charAt(1);
                /* If a transaction is aborted then it will not commit */
                if (!transactions.get(key).get("State").equals("Aborted"))
                    cc.endOrAbort(transactions, locks, key, "Commited", blockedOp);
            }
            cc.printRecords(transactions, locks);
        }
        System.out.println("Output written in output.txt successfully !!");
        fw.close();
    }
    public void beginTransaction(HashMap<String, HashMap<String, Object>> transactions, HashMap<Character, HashMap<String, Object>> locks, String st, int i) throws IOException {
        String key = "T"+st.charAt(1);
        fw.write("This will just create record of "+key+" with active state and timestamp "+i+"\n");
        transactions.put(key, new HashMap<>());
        HashMap<String, Object> map = transactions.get(key);
        map.put("Timestamp", ""+i);
        map.put("State", "Active");
        map.put("Blocked_By", new ArrayList());
        map.put("Blocked_Operations", new ArrayList());
    }
    public void readTransaction(HashMap<String, HashMap<String, Object>> transactions, HashMap<Character, HashMap<String, Object>> locks, String st, HashMap<String, LinkedList<String>> blockedOp) throws IOException {
        String keyForTransaction = "T"+st.charAt(1);
        /*If the transaction is aborted or commited then nothing should happen*/
        if (transactions.get(keyForTransaction).get("State").equals("Aborted") || transactions.get(keyForTransaction).get("State").equals("Commited")) {
            fw.write("Transaction has already been "+transactions.get(keyForTransaction).get("State")+" so nothing will happen \n");
            return;
        }
        /*if transaction is blocked then this operation should be added in the blocked operations' list*/
        if (transactions.get(keyForTransaction).get("State").equals("Blocked")) {
            String transaction = ((ArrayList) transactions.get(keyForTransaction).get("Blocked_By")).get(((ArrayList) transactions.get(keyForTransaction).get("Blocked_By")).size() - 1).toString();
            blockedOp.get(transaction).addLast(st);
            ((ArrayList) transactions.get(keyForTransaction).get("Blocked_Operations")).add(st);
            return;
        }
        char keyForLock = st.charAt(st.indexOf('(')+1);
        if (!locks.containsKey(keyForLock)) {
            locks.put(keyForLock, new HashMap<>());
        }
        HashMap<String, Object> map = locks.get(keyForLock);

        /*if the data item is locked and there it is write locked then according to timestamp the further execution should occur*/
        if (map.containsKey("Lock_Mode") && map.get("Lock_Mode").equals('W')) {
            HashMap<String, Object> mapForLocks = locks.get(keyForLock);
            HashMap<String, Object> mapForTransactions = transactions.get(keyForTransaction);
            ArrayList<String> list = new ArrayList<String>((ArrayList)mapForLocks.get("tid"));
            if (!list.contains(keyForTransaction))
                list.add(keyForTransaction);
            int min = Integer.MAX_VALUE;
            String desiredTid = new String();
            /*For finding the timestamp*/
            for (String l : list) {
                HashMap<String, Object> curr = transactions.get(l);
                int timeStamp = Integer.parseInt(""+curr.get("Timestamp"));
                if (min > timeStamp) {
                    min = timeStamp;
                    desiredTid = l;
                }
            }
            /*if current transaction is not the oldest one among transactions which have locked the data item*/
            if (!desiredTid.equals(keyForTransaction)) {
                mapForTransactions.put("State", "Blocked");
                ((ArrayList) mapForTransactions.get("Blocked_Operations")).add(st);
                ArrayList<String> blockedBy = (ArrayList) mapForLocks.get("tid");
                ArrayList<String> olderTransactions = new ArrayList<>();
                ConcurrencyControl concurrencyControl = new ConcurrencyControl();
                for (String lockKey : blockedBy) {
                    if (Integer.parseInt((String) transactions.get(lockKey).get("Timestamp")) < Integer.parseInt((String) mapForTransactions.get("Timestamp"))) {
                        ((ArrayList) mapForTransactions.get("Blocked_By")).add(lockKey);
                        olderTransactions.add(lockKey);
                        if (!blockedOp.containsKey(lockKey))
                            blockedOp.put(lockKey, new LinkedList<>());
                        blockedOp.get(lockKey).addLast(st);
                    }
                }
                fw.write(keyForTransaction+" is younger than the following transactions "+olderTransactions.toString()+" so it will be blocked and the operation "+st+" will be added to the blocked operations list and these transactions "+olderTransactions.toString()+" will be added to the list of blocked by transactions \n");
                for (String tAbort : list) {
                    if (Integer.parseInt((String) transactions.get(tAbort).get("Timestamp")) > (Integer.parseInt((String) mapForTransactions.get("Timestamp")))) {
                        concurrencyControl.endOrAbort(transactions, locks, tAbort, "Aborted", blockedOp);
                    }
                }
            } else {
                fw.write("Current transaction "+keyForTransaction+" is the oldest one which has locked data item "+keyForLock+" so it will read it and all the other transactions if any, will be aborted \n");
                mapForLocks.put("Lock_Mode", 'R');
                mapForLocks.put("tid", (new ArrayList<>()));
                ((ArrayList) mapForLocks.get("tid")).add(keyForTransaction);
                list.remove(keyForTransaction);
                for (String tAbort : list) {
                    ConcurrencyControl concurrencyControl = new ConcurrencyControl();
                    concurrencyControl.endOrAbort(transactions, locks, tAbort, "Aborted", blockedOp);
                }
            }

        } else {
            fw.write("No transaction has write locked the data item "+keyForLock+" so transaction "+keyForTransaction+" will read it \n");
            map.put("Lock_Mode", 'R');
            if (!map.containsKey("tid")) {
                map.put("tid", new ArrayList<>());
            }
            ArrayList<String> list = (ArrayList)map.get("tid");
            list.add(keyForTransaction);
            map.put("tid", list);
        }
    }
    public void writeTransaction(HashMap<String, HashMap<String, Object>> transactions, HashMap<Character, HashMap<String, Object>> locks, String st, HashMap<String, LinkedList<String>> blockedOp) throws IOException {
        String keyForTransaction = "T"+st.charAt(1);
        char keyForLock = st.charAt(st.indexOf('(')+1);
        /*If the transaction is aborted or commited then nothing should happen*/
        if (transactions.get(keyForTransaction).get("State").equals("Aborted") || transactions.get(keyForTransaction).get("State").equals("Commited")) {
            fw.write("Transaction has already been "+transactions.get(keyForTransaction).get("State")+" so nothing will happen \n");
            return;
        }
        /*if transaction is blocked then this operation should be added in the blocked operations' list*/
        if (transactions.get(keyForTransaction).get("State").equals("Blocked")) {
            String transaction = ((ArrayList) transactions.get(keyForTransaction).get("Blocked_By")).get(((ArrayList) transactions.get(keyForTransaction).get("Blocked_By")).size() - 1).toString();
            blockedOp.get(transaction).addLast(st);
            ((ArrayList) transactions.get(keyForTransaction).get("Blocked_Operations")).add(st);
            fw.write("Transaction is "+transactions.get(keyForTransaction).get("State")+" so nothing will happen \n");
            return;
        }
        /*if the data item is locked then according to timestamp the further execution should occur*/
        if (!locks.isEmpty() && locks.containsKey(keyForLock)) {
            HashMap<String, Object> mapForLocks = locks.get(keyForLock);
            HashMap<String, Object> mapForTransactions = transactions.get(keyForTransaction);
            ArrayList<String> list = (ArrayList)mapForLocks.get("tid");
            ArrayList<String> refList = (ArrayList<String>) list.clone();
            if (!refList.contains(keyForTransaction))
                refList.add(keyForTransaction);
            int min = Integer.MAX_VALUE;
            String desiredTid = new String();
            /*For finding the timestamp*/
            for (String l : refList) {
                HashMap<String, Object> curr = transactions.get(l);
                int timeStamp = Integer.parseInt(""+curr.get("Timestamp"));
                if (min > timeStamp) {
                    min = timeStamp;
                    desiredTid = l;
                }
            }
            /*if current transaction is not the oldest one among transactions which have locked the data item*/
            if (!desiredTid.equals(keyForTransaction)) {
                mapForTransactions.put("State", "Blocked");
                ((ArrayList) mapForTransactions.get("Blocked_Operations")).add(st);
                ArrayList<String> blockedBy = (ArrayList) mapForLocks.get("tid");
                ArrayList<String> olderTransactions = new ArrayList<>();
                ConcurrencyControl concurrencyControl = new ConcurrencyControl();
                for (String lockKey : blockedBy) {
                    if (Integer.parseInt((String) transactions.get(lockKey).get("Timestamp")) < Integer.parseInt((String) mapForTransactions.get("Timestamp"))) {
                        ((ArrayList) mapForTransactions.get("Blocked_By")).add(lockKey);
                        olderTransactions.add(lockKey);
                        if (!blockedOp.containsKey(lockKey))
                            blockedOp.put(lockKey, new LinkedList<>());
                        blockedOp.get(lockKey).addLast(st);
                    }
                }
                fw.write(keyForTransaction+" is younger than the following transactions "+olderTransactions.toString()+" so it will be blocked and the operation "+st+" will be added to the blocked operations list and these transactions "+olderTransactions.toString()+" will be added to the list of blocked by transactions \n");
                for (String tAbort : refList) {
                    if (Integer.parseInt((String) transactions.get(tAbort).get("Timestamp")) > (Integer.parseInt((String) mapForTransactions.get("Timestamp")))) {
                        concurrencyControl.endOrAbort(transactions, locks, tAbort, "Aborted", blockedOp);
                    }
                }
            } else {
                fw.write("Current transaction "+keyForTransaction+" is the oldest one which has locked data item "+keyForLock+" so it will write it and all the other transactions if any, will be aborted \n");
                mapForLocks.put("Lock_Mode", 'W');
                mapForLocks.put("tid", (new ArrayList<>()));
                ((ArrayList) mapForLocks.get("tid")).add(keyForTransaction);
                list.remove(keyForTransaction);
                for (String tAbort : list) {
                    ConcurrencyControl concurrencyControl = new ConcurrencyControl();
                    concurrencyControl.endOrAbort(transactions, locks, tAbort, "Aborted", blockedOp);
                }
            }
        } else {
            fw.write("No transaction has locked the data item "+keyForLock+" so transaction "+keyForTransaction+" will write it \n");
            locks.put(keyForLock, new HashMap<>());
            HashMap<String, Object> mapForLocks = locks.get(keyForLock);
            mapForLocks.put("Lock_Mode", 'W');
            mapForLocks.put("tid", (new ArrayList<>()));
            ((ArrayList) mapForLocks.get("tid")).add(keyForTransaction);
        }
    }

    public void endOrAbort(HashMap<String, HashMap<String, Object>> transactions, HashMap<Character, HashMap<String, Object>> locks, String tr, String condition, HashMap<String, LinkedList<String>> blockedOp) throws IOException {
        /*If the transaction is blocked then it should not commit */
        if (transactions.get(tr).get("State").equals("Blocked") && condition.equals("Commited")) {
            String transaction = ((ArrayList) transactions.get(tr).get("Blocked_By")).get(((ArrayList) transactions.get(tr).get("Blocked_By")).size() - 1).toString();
            blockedOp.get(transaction).addLast(tr);
            ((ArrayList) transactions.get(tr).get("Blocked_Operations")).add("e"+tr.charAt(1));
            fw.write("Transaction is "+transactions.get(tr).get("State")+" so nothing will happen \n");
            return;
        }
        if (condition.equals("Commited")) {
            fw.write("Commiting the transaction " + tr +"\n");
        }
        else {
            fw.write("Aborting the transaction " + tr+"\n");
        }
        /* Updating the Lock Table records with respect to current transaction */
        transactions.get(tr).put("State", condition);
        transactions.get(tr).put("Blocked_By", new ArrayList());
        transactions.get(tr).put("Blocked_Operations", new ArrayList());
        ArrayList<Character> ch = new ArrayList<>();
        for (Character lock : locks.keySet()) {
            HashMap<String, Object> currentLock = locks.get(lock);
            ArrayList<String> tIdList = (ArrayList) currentLock.get("tid");
            if (tIdList.size() > 0 && tIdList.contains(tr)) {
                if (tIdList.size() == 1) {
                     ch.add(lock);
                } else {
                    ((ArrayList)(locks.get(lock).get("tid"))).remove(tr);
                }
            }
        }
        if (ch.size() > 0) {
            for (Character c : ch) {
                locks.remove(c);
            }
        }
        /*Removing the aborted transaction from blocked by list of every transaction and Activating the blocked transactions which are blocked by current transaction*/
        for (String allTransactions : transactions.keySet()) {
            ArrayList list1 = ((ArrayList) transactions.get(allTransactions).get("Blocked_By"));
            if (!list1.isEmpty()) {
                if (list1.contains(tr)) {
                    ((ArrayList) transactions.get(allTransactions).get("Blocked_By")).remove(tr);
                    list1 = ((ArrayList) transactions.get(allTransactions).get("Blocked_By"));
                    if (list1.size() == 0) {
                        transactions.get(allTransactions).put("State", "Active");
                        ArrayList<String> operations = ((ArrayList) transactions.get(allTransactions).get("Blocked_Operations"));
                        ArrayList<String> operationsRef = (ArrayList<String>) operations.clone();
                        for (String op : operationsRef) {
                            if (blockedOp.containsKey(tr) && blockedOp.get(tr).contains(op))
                                ((ArrayList) transactions.get(allTransactions).get("Blocked_Operations")).remove(op);
                        }
                    }
                }
            }
        }
        /*Running their operations which are activated*/
        if (blockedOp.containsKey(tr)) {
            fw.write("Activating if any, the blocked transactions which are blocked by "+tr+" and running their operations \n");
            for (String op : blockedOp.get(tr)) {
                if (transactions.get("T"+op.charAt(1)).get("State").equals("Aborted") || transactions.get("T"+op.charAt(1)).get("State").equals("Commited"))
                    continue;
                fw.write("Blocked operation " + op + " by " + tr + " will be executed \n");
                ConcurrencyControl concurrencyControl = new ConcurrencyControl();
                if (op.charAt(0) == 'r') {
                    concurrencyControl.readTransaction(transactions, locks, op, blockedOp);
                } else if (op.charAt(0) == 'w') {
                    concurrencyControl.writeTransaction(transactions, locks, op, blockedOp);
                } else {
                    concurrencyControl.endOrAbort(transactions, locks, "T"+op.charAt(1), "Commited", blockedOp);
                }
            }
            blockedOp.remove(tr);
        }
    }
    public void printRecords(HashMap<String, HashMap<String, Object>> transactions, HashMap<Character, HashMap<String, Object>> locks) throws IOException {
        fw.write("=====================================transaction============================ \n");
        for (String k : transactions.keySet()) {
            fw.write("Transaction ID: "+k+"\n");
            fw.write("Timestamp: "+transactions.get(k).get("Timestamp")+"\n");
            fw.write("State: "+transactions.get(k).get("State")+"\n");
            fw.write("Blocked By: "+transactions.get(k).get("Blocked_By").toString()+"\n");
            fw.write("Blocked Operations: "+transactions.get(k).get("Blocked_Operations").toString()+"\n");
            fw.write("---------------------------------------\n");
        }
        fw.write("======================Locks======================\n");
        if (locks.isEmpty()) {
            fw.write("Data Item: " + new String()+"\n");
            fw.write("Lock Mode: " + new String()+"\n");
            fw.write("TransactionId: " + new String()+"\n");
        } else {
            for (Character k : locks.keySet()) {
                fw.write("Data Item: " + k+"\n");
                fw.write("Lock Mode: " + locks.get(k).get("Lock_Mode")+"\n");
                fw.write("TransactionId: " + locks.get(k).get("tid").toString()+"\n");
                fw.write("---------------------------------------"+"\n");
            }
        }
        fw.write("============================================"+"\n");
    }
}