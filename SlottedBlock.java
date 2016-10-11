import java.nio.*;
import java.util.*;

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
	//# of entries
	intBuffer.put(0, 0);
	
	//prevBlockId
	intBuffer.put(1,-1);

	//BlockId
	intBuffer.put(2,-1);

	//nextBlockId
	intBuffer.put(3,-1);
	
	//"end" of slot array.
	intBuffer.put(4,-1);
	
    }


    /**
     * Sets the block id.
     * @param blockId the new block id.
     */
    public void setBlockId(int blockId)
    {
	intBuffer.put(2, blockId);
    }

    /**
     * Gets the block id.
     * @return the block id.
     */
    public int getBlockId()
    {
        return intBuffer.get(2);
    }

    /**
     * Sets the next block id.
     * @param blockId the next block id.
     */
    public void setNextBlockId(int blockId)
    {
	intBuffer.put(3, blockId);
    }

    /**
     * Gets the next block id.
     * @return the next block id.
     */
    public int getNextBlockId()
    {
        return intBuffer.get(3);
    }

    /**
     * Sets the previous block id.
     * @param blockId the previous block id.
     */
    public void setPrevBlockId(int blockId)
    {
	intBuffer.put(1, blockId);
    }

    /**
     * Gets the previous block id.
     * @return the previous block id.
     */
    public int getPrevBlockId()
    {
        return intBuffer.get(1);
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
	/* space of meta data = (length of slot array x 2 x size of int) + end of slot array marker + # of entries marker + prev,next,current blockId
	   space of records = loop through meta data and tally up second integers(representing how big the record is).
	*/
	int spaceTaken = 0;
	
	//curIndex starts from 4 to account for meta data (# of entries, prev,next,current blockId spans in indexes 0~3)
	int curIndex = 4;
	Boolean needNewSlot = true;
	while (intBuffer.get(curIndex) != -1) {
	    if(intBuffer.get(curIndex) == 0) {
		needNewSlot = false;
	    }
	    spaceTaken += intBuffer.get(curIndex + 1);
	    curIndex += 2;
	}
	spaceTaken += (curIndex + 1)*4;
	if(needNewSlot) {
	    //each "slot" needs two ints, so add 8 bytes to spaceTaken.
	    spaceTaken += 8;
	}
	return data.length - spaceTaken;
    }
        

    /**
     * Dumps out to the screen the # of entries in the block, the location where
     * the free space starts, the slot array in a readable fashion, and the
     * actual contents of each record. (This method merely exists for debugging
     * and testing purposes.)
    */ 
    public void dumpBlock()
    {
	System.out.println("Number of entries: " + intBuffer.get(0));
	System.out.println("===========contents===========");
	
	int curIndex = 4;

	int offset = 0;
	int length = 0;

	while(intBuffer.get(curIndex) != -1) {
	    if(intBuffer.get(curIndex) == 0) {
		curIndex += 2;
		continue;
	    }
	    offset = intBuffer.get(curIndex)*4;
	    length = intBuffer.get(curIndex + 1);
	    System.out.println("Slot Array Item #: " + ((curIndex/2) - 1) + " | " + "offset: " + offset
			       + " | " + "Length: " + length);
	    System.out.print("Content:           ");
	    for (int i = offset/4; i < (offset+length)/4; i++) {
		System.out.print(intBuffer.get(i) + " | ");
	    }
	    System.out.println("\n ---------------------------");
	    curIndex += 2;
	}
	System.out.println("Free space starts at byte " + (curIndex*4 + 4));
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

	//curIndex starts from 4 to account for meta data (# of entries, prev,next,current blockId spans in indexes 0~3)
	int curIndex = 4;

	while (intBuffer.get(curIndex) != -1) {
	    if(intBuffer.get(curIndex) < smallestIndex) {
		smallestIndex = intBuffer.get(curIndex);
	    }
	    curIndex += 2;
	}
	//assuming all data are ints.
	int index = smallestIndex - record.length/SIZE_OF_INT;
	IntBuffer inputData = (ByteBuffer.wrap(record)).asIntBuffer();
	
	for(int i = 0; i < record.length/SIZE_OF_INT; i++) {
	    intBuffer.put(index+i, inputData.get(i));
	}
	//increment # of entries accordingly.
	intBuffer.put(0, intBuffer.get(0) + 1);


	/*Find a spot to put meta data(slot data) in.
	  If there is an empty slot in the slot array, use that.
	 */
	int slotIndex = 4;
	while (intBuffer.get(slotIndex) != -1) {
	    if(intBuffer.get(slotIndex) == 0) {
		break;
	    }
	    slotIndex += 2;
	}

	//"extend" slot array if necessary.
	if(intBuffer.get(slotIndex) == -1) {
	    intBuffer.put(slotIndex + 2, -1);
	}

	//put index where data starts to slot array
	intBuffer.put(slotIndex, index);

	//put length of the data(in bytes) to slot array
	intBuffer.put(slotIndex + 1, record.length);
	
	RID rid = new RID(this.getBlockId(), (slotIndex/2) - 1);
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
	int slotArrayLength = 0;
	int slotIndex = 4;
	int end_of_free_space = data.length;
	
	//calculate the "length" of the slot array. (two indices makes one slot in the slot array)
	while(intBuffer.get(slotIndex) != -1) {
	    slotArrayLength += 1;
	    if(intBuffer.get(slotIndex) < end_of_free_space && intBuffer.get(slotIndex) > 0) {
		end_of_free_space = intBuffer.get(slotIndex) - 1;
	    }
	    slotIndex += 2;
	}
	//if slot we are looking for never existed.
	if (rid.slotNum > slotArrayLength) {
	    System.out.println("Item # " + rid.slotNum + " does not exist.");
	    return false;
	}
	//if slot we are looking for is already deleted.
	else if (intBuffer.get(rid.slotNum*2 + 2) == 0) {
	    System.out.println("Item # " + rid.slotNum + " is already deleted.");
	    return false;
	}
	//if we are in here, it means item to delete does exist.
	int offset = intBuffer.get(rid.slotNum*2 + 2);
	int length = intBuffer.get(rid.slotNum*2 + 3)/4;

	//delete the slot array.
	intBuffer.put(rid.slotNum*2 + 2, 0);
	intBuffer.put(rid.slotNum*2 + 3, 0);

	intBuffer.put(0, intBuffer.get(0) - 1);

	int dataIndex = offset - 1;
	
	//compact the data accordingly.
	while(dataIndex > end_of_free_space) {
	    intBuffer.put(dataIndex + length, intBuffer.get(dataIndex));
	    dataIndex -= 1;
	}
	//update slot arrays accordingly.
	slotIndex = 4;
	while(intBuffer.get(slotIndex) != -1) {
	    if(intBuffer.get(slotIndex) < offset && intBuffer.get(slotIndex) > 0) {
		intBuffer.put(slotIndex, intBuffer.get(slotIndex) + length);
	    }
	    slotIndex += 2;
	}

	//compact slot array if necessary.
	int prevSlotIndex = slotIndex;
	slotIndex -= 2;
	while(intBuffer.get(slotIndex) == 0 && slotIndex != 4) {
	    slotIndex -= 2;
	}
	intBuffer.put(prevSlotIndex, 0);
	intBuffer.put(slotIndex + 2, -1);


	return true;
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
	
	int firstRecord = 4;
	while(intBuffer.get(firstRecord) != -1) {
	    if (intBuffer.get(firstRecord) != 0) {
		break;
	    }
	    firstRecord += 2;
	}
	RID rid = new RID(this.getBlockId(), (firstRecord/2)-1);
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
	//if curRid is the last record.
	if(intBuffer.get(curRid.slotNum*2 + 4) == -1) {
	    return null;
	}
	
	int nextIndex = curRid.slotNum*2 + 2;
	while(intBuffer.get(nextIndex) != -1) {
	    nextIndex += 2;
	    if(intBuffer.get(nextIndex) != 0) {
		break;
	    }

	}
	RID rid = new RID(this.getBlockId(), (nextIndex/2) - 1);
	return rid;
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
	/*slot Number that points to the index of record we are looking for.
	 */
	int start = intBuffer.get((rid.slotNum+1)*2);

	//End of data index for the record we are trying to get.
	int finish = start + intBuffer.get((rid.slotNum+1)*2 + 1)/SIZE_OF_INT;

	
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
