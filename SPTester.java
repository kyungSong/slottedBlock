import java.io.*;
import java.util.*;
import java.nio.*;
import java.util.Random;

public class SPTester
{
    public static interface Testable
    {
        void test() throws Exception;
    }
    
    public static class TestFailedException extends RuntimeException
    {
        public TestFailedException(String explanation)
        {
            super(explanation);
        }
    }

    public static class Test1 implements Testable
    {
        public void test() throws Exception
        {
            SlottedBlock sp = new SlottedBlock(new Block());
            sp.init();
            
            System.out.println("--- Test 1: Block Initialization Checks ---");
            sp.setBlockId(7);
            sp.setNextBlockId(8);
            sp.setPrevBlockId(SlottedBlock.INVALID_BLOCK);
            
            System.out.println
                ("Current Block No.: " + sp.getBlockId() + ", " +
                 "Next Block Id: " + sp.getNextBlockId() + ", " +
                 "Prev Block Id: " + sp.getPrevBlockId() + ", " +
                 "Available Space: " + sp.getAvailableSpace());
        
            if (!sp.empty())
                throw new TestFailedException("Block should be empty.");

            System.out.println("Block Empty as expected.");
            sp.dumpBlock();
        }
    }


    public static class Test2 implements Testable
    {
        public void test() throws Exception
        {
            int buffSize = 20;
            int limit = 20;
            byte[] tmpBuf = new byte[buffSize];
	    /* code to test if dump block works.
	    int count = 0;
	    for(int i = 0; i < 5; i++) {
		System.arraycopy(ByteBuffer.allocate(4).putInt(i).array(), 0, tmpBuf, count, 4);
		count += 4;
	    }
	    
	    */
            SlottedBlock sp = new SlottedBlock(new Block());
            sp.init();
            sp.setBlockId(7);
            sp.setNextBlockId(8);
            sp.setPrevBlockId(SlottedBlock.INVALID_BLOCK);
            System.out.println("--- Test 2: Insert and traversal of " +
                               "records ---");
            for (int i=0; i < limit; i++)
            {
                RID rid = sp.insertRecord(tmpBuf);
                System.out.println("Inserted record, RID " + rid.blockId +
                                   ", " + rid.slotNum);
                rid = sp.nextRecord(rid);
            }

            if (sp.empty())
                throw new TestFailedException("The block cannot be empty");
            
            RID rid = sp.firstRecord();
            while (rid != null)
            {
                tmpBuf = sp.getRecord(rid); 
                System.out.println("Retrieved record, RID " + rid.blockId +
                                   ", " + rid.slotNum);
                rid = sp.nextRecord(rid);
            }
	    //sp.dumpBlock();
        }
    }

    public static class Test3 implements Testable
    {
	public void test() throws Exception
	{
            int buffSize = 20;
            int limit = 20;
            byte[] tmpBuf = new byte[buffSize];

	    /* code to test if dump block works. */
	    
	    int count = 0;
	    for(int i = 0; i < 5; i++) {
		System.arraycopy(ByteBuffer.allocate(4).putInt(i).array(), 0, tmpBuf, count, 4);
		count += 4;
	    }
	    
            SlottedBlock sp = new SlottedBlock(new Block());
            sp.init();
            sp.setBlockId(7);
            sp.setNextBlockId(8);
            sp.setPrevBlockId(SlottedBlock.INVALID_BLOCK);
            System.out.println("--- Test 3: Insert, traversal and deletion of " +
                               "records ---");
            for (int i=0; i < limit; i++)
            {
                RID rid = sp.insertRecord(tmpBuf);
                System.out.println("Inserted record, RID " + rid.blockId +
                                   ", " + rid.slotNum);
                rid = sp.nextRecord(rid);
            }

            if (sp.empty())
                throw new TestFailedException("The block cannot be empty");
            
            RID rid = sp.firstRecord();
            while (rid != null)
            {
                tmpBuf = sp.getRecord(rid); 
                System.out.println("Retrieved record, RID " + rid.blockId +
                                   ", " + rid.slotNum);
                rid = sp.nextRecord(rid);
            }
	    sp.dumpBlock();

	    System.out.println("--- delete starts ---");
	    
	    //delete last record
	    RID rid_delete = new RID(7, 20);
	    boolean deleted = sp.deleteRecord(rid_delete);
	    if (deleted) {
		System.out.println("Record " + rid_delete.slotNum + " was successfully deleted.");
	    }

	    //delete items that are not last record to last record (to see if compacting slot array works).
	    rid_delete = new RID(7, 17);
	    deleted = sp.deleteRecord(rid_delete);
	    if (deleted) {
		System.out.println("Record " + rid_delete.slotNum + " was successfully deleted.");
	    }
	    rid_delete = new RID(7, 18);
	    deleted = sp.deleteRecord(rid_delete);
	    if (deleted) {
		System.out.println("Record " + rid_delete.slotNum + " was successfully deleted.");
	    }
	    rid_delete = new RID(7, 19);
	    deleted = sp.deleteRecord(rid_delete);
	    if (deleted) {
		System.out.println("Record " + rid_delete.slotNum + " was successfully deleted.");
	    }
	    	    
	    sp.dumpBlock();

	    //try deleting items that don't exist/that are already deleted.
	    rid_delete = new RID(7, 19);
	    deleted = sp.deleteRecord(rid_delete);
	    if (deleted) {
		System.out.println("Record " + rid_delete.slotNum + " was successfully deleted.");
	    }

	    rid_delete = new RID(7, 14);
	    deleted = sp.deleteRecord(rid_delete);
	    if (deleted) {
		System.out.println("Record " + rid_delete.slotNum + " was successfully deleted.");
	    }

	    rid_delete = new RID(7, 14);
	    deleted = sp.deleteRecord(rid_delete);
	    if (deleted) {
		System.out.println("Record " + rid_delete.slotNum + " was successfully deleted.");
	    }
	    
        }
    }
    
public static class Test4 implements Testable
    {
	public void test() throws Exception
	{
            int limit = 20;
	    
            SlottedBlock sp = new SlottedBlock(new Block());
            sp.init();
            sp.setBlockId(7);
            sp.setNextBlockId(8);
            sp.setPrevBlockId(SlottedBlock.INVALID_BLOCK);
	    /* code to test if dump block works. */
	    System.out.println("--- Test 4: Insert, traversal and deletion of " +
                               "variable length records ---");
	    
	    for(int i = 0; i < limit; i++) {
		byte[] tmpBuf = new byte[(i+1)*4];
                RID rid = sp.insertRecord(tmpBuf);
                System.out.println("Inserted record, RID " + rid.blockId +
                                   ", " + rid.slotNum);
                rid = sp.nextRecord(rid);
		
	    }
	    sp.dumpBlock();

            if (sp.empty())
                throw new TestFailedException("The block cannot be empty");

	    System.out.println("--- delete starts ---");
	    
	    //delete last record
	    RID rid_delete = new RID(7, 20);
	    boolean deleted = sp.deleteRecord(rid_delete);
	    if (deleted) {
		System.out.println("Record " + rid_delete.slotNum + " was successfully deleted.");
	    }

	    //delete items that are not last record to last record (to see if compacting slot array works).
	    rid_delete = new RID(7, 17);
	    deleted = sp.deleteRecord(rid_delete);
	    if (deleted) {
		System.out.println("Record " + rid_delete.slotNum + " was successfully deleted.");
	    }
	    rid_delete = new RID(7, 18);
	    deleted = sp.deleteRecord(rid_delete);
	    if (deleted) {
		System.out.println("Record " + rid_delete.slotNum + " was successfully deleted.");
	    }
	    rid_delete = new RID(7, 19);
	    deleted = sp.deleteRecord(rid_delete);
	    if (deleted) {
		System.out.println("Record " + rid_delete.slotNum + " was successfully deleted.");
	    }
	    	    
	    sp.dumpBlock();

	    //try deleting items that don't exist/that are already deleted.
	    rid_delete = new RID(7, 19);
	    deleted = sp.deleteRecord(rid_delete);
	    if (deleted) {
		System.out.println("Record " + rid_delete.slotNum + " was successfully deleted.");
	    }

	    rid_delete = new RID(7, 14);
	    deleted = sp.deleteRecord(rid_delete);
	    if (deleted) {
		System.out.println("Record " + rid_delete.slotNum + " was successfully deleted.");
	    }

	    rid_delete = new RID(7, 14);
	    deleted = sp.deleteRecord(rid_delete);
	    if (deleted) {
		System.out.println("Record " + rid_delete.slotNum + " was successfully deleted.");
		}
	    sp.dumpBlock();
	    
	    }
    }


    public static boolean runTest(Testable testObj)
    {
        boolean success = true;
        try
        {
            testObj.test();
        }
        catch (Exception e)
        {
            success = false;
            e.printStackTrace();
        }

        return success;
    }


    public static void main(String[] args)
    {
        System.out.println("Running block tests.");

         runTest(new Test1());
         runTest(new Test2());
	 runTest(new Test3());
	 runTest(new Test4());
    }
}
