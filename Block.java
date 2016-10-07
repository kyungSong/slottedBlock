/**
 * Class to hold a block's worth of data in memory.
 * @author Dave Musicant, with considerable inspiration from the UW-Madison
 * Minibase project
 */
public class Block
{
    /**
     * Size of a block in bytes.
     */
    public static final int BLOCKSIZE = 1024;

    /**
     * Array to actually contain block data.
     */
    public byte[] data;

    public Block()
    {
        data = new byte[BLOCKSIZE];
    }
}
