import java.nio.*;

/**
 * Slotted file block. This is a wrapper around a traditional Block that
 * adds the appropriate struture to it.
 *
 * @author Dave Musicant, with considerable inspiration from the UW-Madison
 * Minibase project
 */
public class SlottedBlock
{
    public static class BlockFullException extends RuntimeException {};
    public static class BadSlotIdException extends RuntimeException {};
    public static class BadBlockIdException extends RuntimeException {};

    private static class SlotArrayOutOfBoundsException
        extends RuntimeException {};

    /**
     * Value to use for an invalid block id.
     */
    public static final int INVALID_BLOCK = -1;
    public static final int SIZE_OF_INT = 4;

    private byte[] data;
    private IntBuffer intBuffer;
    private int intBufferLength;

    private int blockId;
    private int nextBlockId;
    private int prevBlockId;

    /**
     * Constructs a slotted block by wrapping around a block object already
     * provided.
     * @param block the block to be wrapped.
     */
    public SlottedBlock(Block block)
    {
        data = block.data;
        intBuffer = (ByteBuffer.wrap(data)).asIntBuffer();
        intBufferLength = data.length / SIZE_OF_INT;
    }

    /**
     * Initializes values in the block as necessary. This is separated out from
     * the constructor since it actually modifies the block at hand, where as
     * the constructor simply sets up the mechanism.
     */
    public void init()
    {
	intBuffer.put(0,0);
	//-1 denotes the end of "slot array".
	intBuffer.put(1,-1);
	
    }


    /**
     * Sets the block id.
     * @param blockId the new block id.
     */
    public void setBlockId(int blockId)
    {
	this.blockId = blockId;
    }

    /**
     * Gets the block id.
     * @return the block id.
     */
    public int getBlockId()
    {
        return blockId;
    }

    /**
     * Sets the next block id.
     * @param blockId the next block id.
     */
    public void setNextBlockId(int blockId)
    {
	this.nextBlockId = blockId;
    }

    /**
     * Gets the next block id.
     * @return the next block id.
     */
    public int getNextBlockId()
    {
        return this.nextBlockId;
    }

    /**
     * Sets the previous block id.
     * @param blockId the previous block id.
     */
    public void setPrevBlockId(int blockId)
    {
	this.prevBlockId = blockId;
    }

    /**
     * Gets the previous block id.
     * @return the previous block id.
     */
    public int getPrevBlockId()
    {
        return prevBlockId;
    }

    /**
     * Determines how much space, in bytes, is actually available in the block,
     * which depends on whether or not a new slot in the slot array is
     * needed. If a new spot in the slot array is needed, then the amount of
     * available space has to take this into consideration. In other words, the
     * space you need for the addition to the slot array shouldn't be included
     * as part of the available space, because from the user's perspective, it
     * isn't available for adding data.
     * @return the amount of available space in bytes
     */
    public int getAvailableSpace()
    {
	/* space of meta data = (# of entries x size of int) + end of slot array marker + # of entries marker (both of which takes 4 bytes) 
	   space of records = loop through meta data and tally up second integers(representing how big the record is).
	*/
	int spaceTaken = 8 + intBuffer.get(0)*2*SIZE_OF_INT;
	for (int i = 1; i < 2*intBuffer.get(0); i = i+2) {
	    spaceTaken += intBuffer.get(i + 1);
	}
	return data.length - spaceTaken - 2*SIZE_OF_INT;
    }
        

    /**
     * Dumps out to the screen the # of entries in the block, the location where
     * the free space starts, the slot array in a readable fashion, and the
     * actual contents of each record. (This method merely exists for debugging
     * and testing purposes.)
    */ 
    public void dumpBlock()
    {
    }

    /**
     * Inserts a new record into the block.
     * @param record the record to be inserted. A copy of the data is
     * placed in the block.
     * @return the RID of the new record 
     * @throws BlockFullException if there is not enough room for the
     * record in the block.
    */
    public RID insertRecord(byte[] record)
    {
	if (this.getAvailableSpace() < record.length) {
	    throw new BlockFullException();
	}
	int smallestIndex = intBufferLength;
	for (int i = 1; i < 2*intBuffer.get(0); i = i+2) {
	    if (intBuffer.get(i) < smallestIndex) {
		smallestIndex = intBuffer.get(i);
	    }
	}
	int index = smallestIndex -record.length/SIZE_OF_INT;
	IntBuffer inputData = (ByteBuffer.wrap(record)).asIntBuffer();
	
	for(int i = 0; i < record.length/SIZE_OF_INT; i++) {
	    intBuffer.put(index+i, inputData.get(i));
	}
	intBuffer.put(0, intBuffer.get(0) + 1);
	
	intBuffer.put(intBuffer.get(0)*2 - 1, index);
	intBuffer.put(intBuffer.get(0)*2, record.length);
	
	RID rid = new RID(this.getBlockId(), intBuffer.get(0));
	return rid;
    }

    /**
     * Deletes the record with the given RID from the block, compacting
     * the hole created. Compacting the hole, in turn, requires that
     * all the offsets (in the slot array) of all records after the
     * hole be adjusted by the size of the hole, because you are
     * moving these records to "fill" the hole. You should leave a
     * "hole" in the slot array for the slot which pointed to the
     * deleted record, if necessary, to make sure that the rids of the
     * remaining records do not change. The slot array should be
     * compacted only if the record corresponding to the last slot is
     * being deleted.
     * @param rid the RID to be deleted.
     * @return true if successful, false if the rid is actually not
     * found in the block.
    */
    public boolean deleteRecord(RID rid)
    {
        return false;
    }

    /**
     * Returns RID of first record in block. Remember that some slots may be
     * empty, so you should skip over these.
     * @return the RID of the first record in the block. Returns null
     * if the block is empty.
     */
    public RID firstRecord()
    {
	if(this.empty()) {
	    return null;
	}
	int firstRecord = 0;
	for (int i = 1; i < 2*intBuffer.get(0); i = i+2) {
	    firstRecord += 1;
	    if (intBuffer.get(i) != 0) {
		break;
	    }
	}
	RID rid = new RID(this.getBlockId(), firstRecord);
	return rid;
    }

    /**
     * Returns RID of next record in the block, where "next in the block" means
     * "next in the slot array after the rid passed in." Remember that some
     * slots may be empty, so you should skip over these.
     * @param curRid an RID
     * @return the RID immediately following curRid. Returns null if
     * curRID is the last record in the block.
     * @throws BadBlockIdException if the block id within curRid is
     * invalid
     * @throws BadSlotIdException if the slot id within curRid is invalid
    */
    public RID nextRecord(RID curRid)
    {
	
	return null;
    }

    /**
     * Returns the record associated with an RID.
     * @param rid the rid of interest
     * @return a byte array containing a copy of the record. The array
     * has precisely the length of the record (there is no padded space).
     * @throws BadBlockIdException if the block id within curRid is
     * invalid
     * @throws BadSlotIdException if the slot id within curRid is invalid
    */
    public byte[] getRecord(RID rid)
    {
	if(rid.blockId == -1) {
	    throw new BadBlockIdException();
	} else if(rid.slotNum == -1) {
	    throw new BadSlotIdException();
	}
	int start = intBuffer.get(rid.slotNum);
	int finish = start + intBuffer.get(rid.slotNum + 1)/SIZE_OF_INT;
	byte[] returnArray = new byte[(finish-start)*SIZE_OF_INT];
	int count = 0;
	for(int i = start; i < finish; i++) {
	    System.arraycopy(ByteBuffer.allocate(4).putInt(intBuffer.get(i)).array(), 0, returnArray, count, SIZE_OF_INT);
	    count += SIZE_OF_INT;
	}
	return returnArray;
    }

    /**
     * Whether or not the block is empty.
     * @return true if the block is empty, false otherwise.
     */
    public boolean empty()
    {
	if(intBuffer.get(0) != 0) {
	    return false;
	}
	return true;
    }
}
