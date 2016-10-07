/**
 * Record identifier. Identifies a record id within a heap file. Contains a block
 * id and a slot number.
 * @author Dave Musicant, with considerable material reused from the
 * UW-Madison Minibase project
 */
public class RID
{
    /**
     * Block identification number.
     */
    public int blockId;

    /**
     * Slot number with a heap file block.
     */
    public int slotNum;

    /**
     * Constructor.
     * @param blockId the block id.
     * @param slotNum the slot number.
     */
    public RID(int blockId, int slotNum)
    {
        this.blockId = blockId;
        this.slotNum = slotNum;
    }
}
