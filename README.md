# slottedBlock
Representation of slotted blocks used in some databases for storing data.

each block is 1024 bytes.

SlottedBlock.java is the wrapper class that gives each block a structure like those used in some databases. Each SlottedBlock holds 5 markers, which are:

1. Number of current entries.
2. Id of previous block.
3. Id of current block.
4. Id of next block.
5. The last slot currently being used.

Blocks auto compacts when an item is deleted.
