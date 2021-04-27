import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Scanner;

// Chase VanderZwan, partners with Neil Patel
// 315-01

public class lab5 {

    private int line_number;

    private int[] cache1;
    private int[] cache2;
    private int[] cache3;
    private AssociativeCache twoWayOneWord;
    private AssociativeCache fourWayOneWord;
    private AssociativeCache fourWayFourWord;
    private int[] cache7;

    private int hits1;
    private int hits2;
    private int hits3;
    private int hits7;

    private int misses1;
    private int misses2;
    private int misses3;
    private int misses7;

    private int index1_size;
    private int index2_size;
    private int index3_size;
    private int index7_size;

    private int tag1_size;
    private int tag2_size;
    private int tag3_size;
    private int tag7_size;

    private lab5(AssociativeCache twoWayOneWord, AssociativeCache fourWayOneWord, AssociativeCache fourWayFourWord) {
        this.line_number = 0;

        this.cache1 = new int[512];    // ( 2048B / 4B ) = 512
        this.cache2 = new int[256];    // ( 2048B / 8B ) = 256
        this.cache3 = new int[128];    // ( 2048 / 16B ) = 128
        this.twoWayOneWord = twoWayOneWord;  // created in main (256x2) --> 2 arrays of 256 indices containing 4B (1 word)
        this.fourWayOneWord = fourWayOneWord; // created in main (128x4) --> 4 arrays of 128 indices containing 4B (1 word)
        this.fourWayFourWord = fourWayFourWord; // created in main (32x4) --> 4 arrays of 32 indices containing 16B (4 words)
        this.cache7 = new int[1024];   // ( 4096B / 4B ) = 1024

        this.hits1 = 0;
        this.hits2 = 0;
        this.hits3 = 0;
        this.hits7 = 0;

        this.misses1 = 0;
        this.misses2 = 0;
        this.misses3 = 0;
        this.misses7 = 0;

        this.index1_size = 9;  // ( 2048B / 4B ) --> log(512) = 9
        this.index2_size = 8;  // ( 2048B / 8B ) --> log(256) = 8
        this.index3_size = 7;  // ( 2048B / 16B ) --> log(128) = 7
        this.index7_size = 10; // ( 4096B / 4B ) --> log(1024) = 10

        this.tag1_size = 21;   // 32 - 9 - 2      = 21
        this.tag2_size = 21;   // 32 - 8 - 1 - 2  = 21
        this.tag3_size = 21;   // 32 - 7 - 2 - 2  = 21
        this.tag7_size = 20;   // 32 - 10 - 2     = 20
    }

    private static class AssociativeCache {
        private int hits;
        private int misses;
        private int index_size;
        private int tag_size;
        private int[][] cache;
        private int[][] LRU;

        private AssociativeCache(int hits, int misses, int index_size, int tag_size, int[][]cache, int[][]LRU){
            this.hits = hits;
            this.misses = misses;
            this.index_size = index_size;
            this.tag_size = tag_size;
            this.cache = cache;
            this.LRU = LRU;
        }
    }

    public static void main(String[] args) {

        final int twoWayOneWord_index_size = 8;   // (2048B / 2arrays) --> 1024; (1024B / 4B) --> log(256) = 8
        final int twoWayOneWord_tag_size = 22;    // 32 - 8 (idx) - 2 (ByteOffset) = 22
        final int fourWayOneWord_index_size = 7;  // (2048B / 4arrays) --> 512; (512B / 4B) --> log(128) = 7
        final int fourWayOneWord_tag_size = 23;   // 32 - 7 (idx) - 2 (ByteOffset) = 23
        final int fourWayFourWord_index_size = 5;  // (2048B / 4arrays) --> 512; (512B / 16B) --> log(32) = 5
        final int fourWayFourWord_tag_size = 23;   // 32 - 5 (idx) - 2 (ByteOffset) - 2 (BlockOffset) = 23

        if (args.length == 0)
            System.out.println("ERROR: lab5.java stream");

        File stream = new File(args[0]);
        Scanner streamScanner = readFile(stream);

        // two way assoc. one word blocks
        int[][] cache4 = new int[256][2];
        int[][] LRU1 = new int[256][2];
        AssociativeCache twoWayOneWord = new AssociativeCache(0, 0, twoWayOneWord_index_size, twoWayOneWord_tag_size, cache4, LRU1);

        // four way assoc. one word blocks
        int[][] cache5 = new int[128][4];
        int[][] LRU2 = new int[128][4];
        AssociativeCache fourWayOneWord = new AssociativeCache(0, 0, fourWayOneWord_index_size, fourWayOneWord_tag_size, cache5, LRU2);

        // four way assoc. four word blocks
        int[][] cache6 = new int[32][4];
        int[][] LRU3 = new int[32][4];
        AssociativeCache fourWayFourWord = new AssociativeCache(0, 0, fourWayFourWord_index_size, fourWayFourWord_tag_size, cache6, LRU3);

        // main object
        lab5 cache_sim = new lab5(twoWayOneWord, fourWayOneWord, fourWayFourWord);

        // init values = -1 (garbage data)
        Arrays.fill(cache_sim.cache1, -1);
        Arrays.fill(cache_sim.cache2, -1);
        Arrays.fill(cache_sim.cache3, -1);
        Arrays.stream(cache_sim.twoWayOneWord.cache).forEach(a -> Arrays.fill(a, -1));
        Arrays.stream(cache_sim.fourWayOneWord.cache).forEach(a -> Arrays.fill(a, -1));
        Arrays.stream(cache_sim.fourWayFourWord.cache).forEach(a -> Arrays.fill(a, -1));
        Arrays.fill(cache_sim.cache7, -1);

        // run program
        readStream(streamScanner, cache_sim);
        printOutput(cache_sim);
    }

    private static void readStream(Scanner streamScanner, lab5 lab){

        while (streamScanner.hasNextLine()){
            lab.line_number ++;
            String line = streamScanner.nextLine();

            // separate valid bit (idx 0) from data (idx 1)
            String[] arr = line.split("\\s+");

            // isolate the tag and index sections and convert them from hex to binary to decimal
            String AddressBits = new BigInteger(arr[1], 16).toString(2);
            StringBuilder AddressBitsFinal = new StringBuilder(32);

            // adds missing trailing 0's. Converts into full 32 bit data
            while (AddressBitsFinal.length() + AddressBits.length() != 32)
                AddressBitsFinal.append("0");
            AddressBitsFinal.append(AddressBits);

            /////////////
            // CACHE 1 //
            /////////////

            String tag1_string = AddressBitsFinal.substring(0, lab.tag1_size);
            String idx1_string = AddressBitsFinal.substring(lab.tag1_size, lab.tag1_size + lab.index1_size);
            int tag1 = Integer.parseInt(tag1_string, 2);
            int index1 = (Integer.parseInt(idx1_string, 2)) % lab.cache1.length;

            if (lab.cache1[index1] == tag1)
                lab.hits1 ++;
            else
            {
                lab.cache1[index1] = tag1;
                lab.misses1++;
            }

            /////////////
            // CACHE 2 //
            /////////////

            String tag2_string = AddressBitsFinal.substring(0, lab.tag2_size);
            String idx2_string = AddressBitsFinal.substring(lab.tag2_size, lab.tag2_size + lab.index2_size);
            int tag2 = Integer.parseInt(tag2_string, 2);
            int index2 = (Integer.parseInt(idx2_string, 2)) % lab.cache2.length;

            if (lab.cache2[index2] == tag2)
                lab.hits2 ++;
            else
            {
                lab.cache2[index2] = tag2;
                lab.misses2++;
            }

            /////////////
            // CACHE 3 //
            /////////////

            String tag3_string = AddressBitsFinal.substring(0, lab.tag3_size);
            String idx3_string = AddressBitsFinal.substring(lab.tag3_size, lab.tag3_size + lab.index3_size);
            int tag3 = Integer.parseInt(tag3_string, 2);
            int index3 = (Integer.parseInt(idx3_string, 2)) % lab.cache3.length;

            if (lab.cache3[index3] == tag3)
                lab.hits3 ++;
            else
            {
                lab.cache3[index3] = tag3;
                lab.misses3++;
            }

            /////////////
            // CACHE 4 //
            /////////////

            String tag4_string = AddressBitsFinal.substring(0, lab.twoWayOneWord.tag_size);
            String idx4_string = AddressBitsFinal.substring(lab.twoWayOneWord.tag_size, lab.twoWayOneWord.tag_size + lab.twoWayOneWord.index_size);
            int tag4 = Integer.parseInt(tag4_string, 2);
            int index4 = (Integer.parseInt(idx4_string, 2)) % lab.twoWayOneWord.cache.length;

            // HIT on Array 1 @ index
            if (lab.twoWayOneWord.cache[index4][0] == tag4)
            {
                lab.twoWayOneWord.hits++;
                lab.twoWayOneWord.LRU[index4][0] = lab.line_number;
            }

            // HIT on Array 2 @ index
            else if (lab.twoWayOneWord.cache[index4][1] == tag4)
            {
                lab.twoWayOneWord.hits++;
                lab.twoWayOneWord.LRU[index4][1] = lab.line_number;
            }

            // MISS >> determine which column to replace
            else
            {
                // CASE: Array 1 is unused  << OR >>  value in Array 1 was used less recently then value in Array 2
                if (lab.twoWayOneWord.cache[index4][0] == -1 || lab.twoWayOneWord.LRU[index4][0] < lab.twoWayOneWord.LRU[index4][1]) {
                    lab.twoWayOneWord.cache[index4][0] = tag4;
                    lab.twoWayOneWord.LRU[index4][0] = lab.line_number;
                }

                // CASE: Array 2 will be replaced
                else {
                    lab.twoWayOneWord.cache[index4][1] = tag4;
                    lab.twoWayOneWord.LRU[index4][1] = lab.line_number;
                }

                lab.twoWayOneWord.misses++;
            }

            /////////////
            // CACHE 5 //
            /////////////

            String tag5_string = AddressBitsFinal.substring(0, lab.fourWayOneWord.tag_size);
            String idx5_string = AddressBitsFinal.substring(lab.fourWayOneWord.tag_size, lab.fourWayOneWord.tag_size + lab.fourWayOneWord.index_size);
            int tag5 = Integer.parseInt(tag5_string, 2);
            int index5 = (Integer.parseInt(idx5_string, 2)) % lab.fourWayOneWord.cache.length;

            // HIT on Array 1 @ index
            if (lab.fourWayOneWord.cache[index5][0] == tag5)
            {
                lab.fourWayOneWord.hits++;
                lab.fourWayOneWord.LRU[index5][0] = lab.line_number;
            }
            // HIT on Array 2 @ index
            else if (lab.fourWayOneWord.cache[index5][1] == tag5)
            {
                lab.fourWayOneWord.hits++;
                lab.fourWayOneWord.LRU[index5][1] = lab.line_number;
            }
            // HIT on Array 3 @ index
            else if (lab.fourWayOneWord.cache[index5][2] == tag5)
            {
                lab.fourWayOneWord.hits++;
                lab.fourWayOneWord.LRU[index5][2] = lab.line_number;
            }
            // HIT on Array 4 @ index
            else if (lab.fourWayOneWord.cache[index5][3] == tag5)
            {
                lab.fourWayOneWord.hits++;
                lab.fourWayOneWord.LRU[index5][3] = lab.line_number;
            }

            // MISS >> determine which column to replace
            else
            {
                // CASE: Array 1 is unused
                if (lab.fourWayOneWord.cache[index5][0] == -1) {
                    lab.fourWayOneWord.cache[index5][0] = tag5;
                    lab.fourWayOneWord.LRU[index5][0] = lab.line_number;
                }
                // CASE: Array 2 is unused
                else if (lab.fourWayOneWord.cache[index5][1] == -1) {
                    lab.fourWayOneWord.cache[index5][1] = tag5;
                    lab.fourWayOneWord.LRU[index5][1] = lab.line_number;
                }
                // CASE: Array 3 is unused
                else if (lab.fourWayOneWord.cache[index5][2] == -1) {
                    lab.fourWayOneWord.cache[index5][2] = tag5;
                    lab.fourWayOneWord.LRU[index5][2] = lab.line_number;
                }
                // CASE: Array 4 is unused
                else if (lab.fourWayOneWord.cache[index5][3] == -1) {
                    lab.fourWayOneWord.cache[index5][3] = tag5;
                    lab.fourWayOneWord.LRU[index5][3] = lab.line_number;
                }

                // CASE: All arrays are occupied >> find least recently used value
                else {
                    int arrMin = findMin(lab.fourWayOneWord.LRU[index5][0], lab.fourWayOneWord.LRU[index5][1], lab.fourWayOneWord.LRU[index5][2], lab.fourWayOneWord.LRU[index5][3]);
                    lab.fourWayOneWord.cache[index5][arrMin] = tag5;
                    lab.fourWayOneWord.LRU[index5][arrMin] = lab.line_number;
                }

                lab.fourWayOneWord.misses++;
            }

            /////////////
            // CACHE 6 //
            /////////////

            String tag6_string = AddressBitsFinal.substring(0, lab.fourWayFourWord.tag_size);
            String idx6_string = AddressBitsFinal.substring(lab.fourWayFourWord.tag_size, lab.fourWayFourWord.tag_size + lab.fourWayFourWord.index_size);
            int tag6 = Integer.parseInt(tag6_string, 2);
            int index6 = (Integer.parseInt(idx6_string, 2)) % lab.fourWayFourWord.cache.length;

            // HIT on Array 1 @ index
            if (lab.fourWayFourWord.cache[index6][0] == tag6)
            {
                lab.fourWayFourWord.hits++;
                lab.fourWayFourWord.LRU[index6][0] = lab.line_number;
            }
            // HIT on Array 2 @ index
            else if (lab.fourWayFourWord.cache[index6][1] == tag6)
            {
                lab.fourWayFourWord.hits++;
                lab.fourWayFourWord.LRU[index6][1] = lab.line_number;
            }
            // HIT on Array 3 @ index
            else if (lab.fourWayFourWord.cache[index6][2] == tag6)
            {
                lab.fourWayFourWord.hits++;
                lab.fourWayFourWord.LRU[index6][2] = lab.line_number;
            }
            // HIT on Array 4 @ index
            else if (lab.fourWayFourWord.cache[index6][3] == tag6)
            {
                lab.fourWayFourWord.hits++;
                lab.fourWayFourWord.LRU[index6][3] = lab.line_number;
            }

            // MISS >> determine which column to replace
            else
            {
                // CASE: Array 1 is unused
                if (lab.fourWayFourWord.cache[index6][0] == -1) {
                    lab.fourWayFourWord.cache[index6][0] = tag6;
                    lab.fourWayFourWord.LRU[index6][0] = lab.line_number;
                }
                // CASE: Array 2 is unused
                else if (lab.fourWayFourWord.cache[index6][1] == -1) {
                    lab.fourWayFourWord.cache[index6][1] = tag6;
                    lab.fourWayFourWord.LRU[index6][1] = lab.line_number;
                }
                // CASE: Array 3 is unused
                else if (lab.fourWayFourWord.cache[index6][2] == -1) {
                    lab.fourWayFourWord.cache[index6][2] = tag6;
                    lab.fourWayFourWord.LRU[index6][2] = lab.line_number;
                }
                // CASE: Array 4 is unused
                else if (lab.fourWayFourWord.cache[index6][3] == -1) {
                    lab.fourWayFourWord.cache[index6][3] = tag6;
                    lab.fourWayFourWord.LRU[index6][3] = lab.line_number;
                }

                // CASE: All arrays are occupied >> find least recently used value
                else {
                    int arrMin = findMin(lab.fourWayFourWord.LRU[index6][0], lab.fourWayFourWord.LRU[index6][1], lab.fourWayFourWord.LRU[index6][2], lab.fourWayFourWord.LRU[index6][3]);
                    lab.fourWayFourWord.cache[index6][arrMin] = tag6;
                    lab.fourWayFourWord.LRU[index6][arrMin] = lab.line_number;
                }

                lab.fourWayFourWord.misses++;
            }

            /////////////
            // CACHE 7 //
            /////////////

            String tag7_string = AddressBitsFinal.substring(0, lab.tag7_size);
            String idx7_string = AddressBitsFinal.substring(lab.tag7_size, lab.tag7_size + lab.index7_size);
            int tag7 = Integer.parseInt(tag7_string, 2);
            int index7 = (Integer.parseInt(idx7_string, 2)) % lab.cache7.length;

            if (lab.cache7[index7] == tag7)
                lab.hits7 ++;
            else
            {
                lab.cache7[index7] = tag7;
                lab.misses7++;
            }

        }

    }

    private static Scanner readFile(File file) {
        Scanner outScanner = null;

        try {
            outScanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            System.out.println("ERROR: File not found for: " + file);
            System.exit(1);
        }

        return outScanner;
    }

    // determines which array as the smallest LRU value and returns the array#
    private static int findMin(int a, int b, int c, int d){
        if (a < b && a < c && a < d)
            return 0;
        if (b < c && b < d)
            return 1;
        if (c < d)
            return 2;
        return 3;
    }

    private static void printOutput(lab5 lab) {
        System.out.println("Cache #1");
        System.out.println("Cache size: 2048B\tAssociativity: 1\tBlock size: 1");
        System.out.printf("Hits: %d\tHit Rate: %.2f", lab.hits1, (((float)lab.hits1/(float)(lab.hits1+lab.misses1))*100));
        System.out.println("%");
        System.out.println("---------------------------");
        System.out.println("Cache #2");
        System.out.println("Cache size: 2048B\tAssociativity: 1\tBlock size: 2");
        System.out.printf("Hits: %d\tHit Rate: %.2f", lab.hits2, (((float)lab.hits2/(float)(lab.hits2+lab.misses2))*100));
        System.out.println("%");
        System.out.println("---------------------------");
        System.out.println("Cache #3");
        System.out.println("Cache size: 2048B\tAssociativity: 1\tBlock size: 4");
        System.out.printf("Hits: %d\tHit Rate: %.2f", lab.hits3, (((float)lab.hits3/(float)(lab.hits3+lab.misses3))*100));
        System.out.println("%");
        System.out.println("---------------------------");
        System.out.println("Cache #4");
        System.out.println("Cache size: 2048B\tAssociativity: 2\tBlock size: 1");
        System.out.printf("Hits: %d\tHit Rate: %.2f", lab.twoWayOneWord.hits, (((float)lab.twoWayOneWord.hits/(float)(lab.twoWayOneWord.hits+lab.twoWayOneWord.misses))*100));
        System.out.println("%");
        System.out.println("---------------------------");
        System.out.println("Cache #5");
        System.out.println("Cache size: 2048B\tAssociativity: 4\tBlock size: 1");
        System.out.printf("Hits: %d\tHit Rate: %.2f", lab.fourWayOneWord.hits, (((float)lab.fourWayOneWord.hits/(float)(lab.fourWayOneWord.hits+lab.fourWayOneWord.misses))*100));
        System.out.println("%");
        System.out.println("---------------------------");
        System.out.println("Cache #6");
        System.out.println("Cache size: 2048B\tAssociativity: 4\tBlock size: 4");
        System.out.printf("Hits: %d\tHit Rate: %.2f", lab.fourWayFourWord.hits, (((float)lab.fourWayFourWord.hits/(float)(lab.fourWayFourWord.hits+lab.fourWayFourWord.misses))*100));
        System.out.println("%");
        System.out.println("---------------------------");
        System.out.println("Cache #7");
        System.out.println("Cache size: 4096B\tAssociativity: 1\tBlock size: 1");
        System.out.printf("Hits: %d\tHit Rate: %.2f", lab.hits7, (((float)lab.hits7/(float)(lab.hits7+lab.misses7))*100));
        System.out.println("%");
        System.out.println("---------------------------");
    }
}
